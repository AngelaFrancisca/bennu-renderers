package pt.ist.fenixWebFramework.servlets.filters.contentRewrite;

import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;

import pt.ist.fenixWebFramework.FenixWebFramework;
import pt.ist.fenixWebFramework.security.User;
import pt.ist.fenixWebFramework.security.UserView;
import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.RequestRewriterFilter.RequestRewriter;

public class GenericChecksumRewriter extends RequestRewriter {

    public static final String CHECKSUM_ATTRIBUTE_NAME = "_request_checksum_";

    public static final String NO_CHECKSUM_PREFIX = "<!-- NO_CHECKSUM -->";

    protected static final int LENGTH_OF_NO_CHECKSUM_PREFIX = NO_CHECKSUM_PREFIX.length();

    private static String calculateChecksum(final StringBuilder source, final int start, final int end) {
	return calculateChecksum(source.substring(start, end));
    }

    private static boolean isRelevantPart(final String part) {
	return part.length() > 0 && !part.startsWith(CHECKSUM_ATTRIBUTE_NAME) && !part.startsWith("page=")
		&& !part.startsWith("org.apache.struts.action.LOCALE") && !part.startsWith("javax.servlet.request.")
		&& !part.startsWith("ok");
    }

    private static String calculateChecksum(final TreeSet<String> strings) {
	final StringBuilder stringBuilder = new StringBuilder();
	for (final String string : strings) {
	    stringBuilder.append(string);
	}
	
	final User user = UserView.getUser();
	if (user != null) {
	    stringBuilder.append(user.getPrivateConstantForDigestCalculation());
	}
	final String checksum = new String(DigestUtils.shaHex(stringBuilder.toString()));
	// System.out.println("Generating checksum for: " +
	// stringBuilder.toString() + " --> " + checksum);
	return checksum;
    }

    public static String calculateChecksum(final String requestString) {
	final int indexLastCardinal = requestString.lastIndexOf('#');
	final String string = indexLastCardinal >= 0 ? requestString.substring(0, indexLastCardinal) : requestString;
	final String[] parts = string.split("\\?|&amp;|&");

	final TreeSet<String> strings = new TreeSet<String>();
	for (final String part : parts) {
	    if (isRelevantPart(part)) {
		final int indexOfEquals = part.indexOf('=');
		if (indexOfEquals >= 0) {
		    strings.add(part.substring(0, indexOfEquals));
		    strings.add(part.substring(indexOfEquals + 1, part.length()));
		} else {
		    strings.add(part);
		}
	    }
	}
	return calculateChecksum(strings);
    }

    public GenericChecksumRewriter(HttpServletRequest httpServletRequest) {
	super(httpServletRequest);
    }

    @Override
    public StringBuilder rewrite(StringBuilder source) {
	if (isRedirectRequest(httpServletRequest)) {
	    return source;
	}
	final StringBuilder response = new StringBuilder();

	int iOffset = 0;

	while (true) {

	    final int indexOfAopen = source.indexOf("<a ", iOffset);
	    final int indexOfFormOpen = source.indexOf("<form ", iOffset);
	    final int indexOfImgOpen = source.indexOf("<img ", iOffset);
	    final int indexOfAreaOpen = source.indexOf("<area ", iOffset);
	    if (indexOfAopen >= 0 && (indexOfFormOpen < 0 || indexOfAopen < indexOfFormOpen)
		    && (indexOfImgOpen < 0 || indexOfAopen < indexOfImgOpen)
		    && (indexOfAreaOpen < 0 || indexOfAopen < indexOfAreaOpen)) {
		if (!isPrefixed(source, indexOfAopen)) {
		    final int indexOfAclose = source.indexOf(">", indexOfAopen);
		    if (indexOfAclose >= 0) {
			final int indexOfHrefBodyStart = findHrefBodyStart(source, indexOfAopen, indexOfAclose);
			if (indexOfHrefBodyStart >= 0) {
			    final char hrefBodyStartChar = source.charAt(indexOfHrefBodyStart - 1);
			    final int indexOfHrefBodyEnd = findHrefBodyEnd(source, indexOfHrefBodyStart, hrefBodyStartChar);
			    if (indexOfHrefBodyEnd >= 0) {

				int indexOfJavaScript = source.indexOf("javascript:", indexOfHrefBodyStart);
				int indexOfMailto = source.indexOf("mailto:", indexOfHrefBodyStart);
				int indexOfHttp = source.indexOf("http://", indexOfHrefBodyStart);
				int indexOfHttps = source.indexOf("https://", indexOfHrefBodyStart);
				if ((indexOfJavaScript < 0 || indexOfJavaScript > indexOfHrefBodyEnd)
					&& (indexOfMailto < 0 || indexOfMailto > indexOfHrefBodyEnd)
					&& (indexOfHttp < 0 || indexOfHttp > indexOfHrefBodyEnd)
					&& (indexOfHttps < 0 || indexOfHttps > indexOfHrefBodyEnd)) {

				    final int indexOfCardinal = source.indexOf("#", indexOfHrefBodyStart);
				    boolean hasCardinal = indexOfCardinal > indexOfHrefBodyStart
					    && indexOfCardinal < indexOfHrefBodyEnd;
				    if (hasCardinal) {
					response.append(source, iOffset, indexOfCardinal);
				    } else {
					response.append(source, iOffset, indexOfHrefBodyEnd);
				    }

				    final String checksum = calculateChecksum(source, indexOfHrefBodyStart, indexOfHrefBodyEnd);
				    final int indexOfQmark = source.indexOf("?", indexOfHrefBodyStart);
				    if (indexOfQmark == -1 || indexOfQmark > indexOfHrefBodyEnd) {
					response.append('?');
				    } else {
					response.append("&amp;");
				    }
				    response.append(CHECKSUM_ATTRIBUTE_NAME);
				    response.append("=");
				    response.append(checksum);

				    if (hasCardinal) {
					response.append(source, indexOfCardinal, indexOfHrefBodyEnd);
				    }

				    final int nextChar = indexOfAclose + 1;
				    response.append(source, indexOfHrefBodyEnd, nextChar);
				    // rewrite(response, source, nextChar);
				    // return;
				    iOffset = nextChar;
				    continue;
				} else {
				    final int nextIndex;

				    if (indexOfJavaScript < 0) {
					indexOfJavaScript = Integer.MAX_VALUE;
				    }
				    if (indexOfMailto < 0) {
					indexOfMailto = Integer.MAX_VALUE;
				    }
				    if (indexOfHttp < 0) {
					indexOfHttp = Integer.MAX_VALUE;
				    }
				    if (indexOfHttps < 0) {
					indexOfHttps = Integer.MAX_VALUE;
				    }

				    nextIndex = min(indexOfJavaScript, indexOfMailto, indexOfHttp, indexOfHttps);

				    response.append(source, iOffset, nextIndex);
				    iOffset = nextIndex;
				    continue;
				}
			    }
			} else {
			    iOffset = continueToNextToken(response, source, iOffset, indexOfAopen);
			    continue;
			}
		    }
		} else {
		    iOffset = continueToNextToken(response, source, iOffset, indexOfAopen);
		    continue;
		}
	    } else if (indexOfFormOpen >= 0 && (indexOfImgOpen < 0 || indexOfFormOpen < indexOfImgOpen)
		    && (indexOfAreaOpen < 0 || indexOfFormOpen < indexOfAreaOpen)) {
		if (!isPrefixed(source, indexOfFormOpen)) {
		    final int indexOfFormClose = source.indexOf(">", indexOfFormOpen);
		    if (indexOfFormClose >= 0) {
			final int indexOfFormActionBodyStart = findFormActionBodyStart(source, indexOfFormOpen, indexOfFormClose);
			if (indexOfFormActionBodyStart >= 0) {
			    final int indexOfFormActionBodyEnd = findFormActionBodyEnd(source, indexOfFormActionBodyStart);
			    if (indexOfFormActionBodyEnd >= 0) {
				final int nextChar = indexOfFormClose + 1;
				response.append(source, iOffset, nextChar);
				final String checksum = calculateChecksum(source, indexOfFormActionBodyStart,
					indexOfFormActionBodyEnd);
				response.append("<input type=\"hidden\" name=\"");
				response.append(CHECKSUM_ATTRIBUTE_NAME);
				response.append("\" value=\"");
				response.append(checksum);
				response.append("\"/>");
				// rewrite(response, source, nextChar);
				// return;
				iOffset = nextChar;
				continue;
			    }
			}
		    }
		} else {
		    iOffset = continueToNextToken(response, source, iOffset, indexOfFormOpen);
		    continue;
		}
	    } else if (indexOfImgOpen >= 0 && (indexOfAreaOpen < 0 || indexOfImgOpen < indexOfAreaOpen)) {
		if (!isPrefixed(source, indexOfImgOpen)) {
		    final int indexOfImgClose = source.indexOf(">", indexOfImgOpen);
		    if (indexOfImgClose >= 0) {
			final int indexOfSrcBodyStart = findSrcBodyStart(source, indexOfImgOpen, indexOfImgClose);
			if (indexOfSrcBodyStart >= 0) {
			    final int indexOfSrcBodyEnd = findSrcBodyEnd(source, indexOfSrcBodyStart);
			    if (indexOfSrcBodyEnd >= 0) {
				response.append(source, iOffset, indexOfSrcBodyEnd);

				final String checksum = calculateChecksum(source, indexOfSrcBodyStart, indexOfSrcBodyEnd);
				final int indexOfQmark = source.indexOf("?", indexOfSrcBodyStart);
				if (indexOfQmark == -1 || indexOfQmark > indexOfSrcBodyEnd) {
				    response.append('?');
				} else {
				    response.append("&amp;");
				}
				response.append(CHECKSUM_ATTRIBUTE_NAME);
				response.append("=");
				response.append(checksum);

				final int nextChar = indexOfImgClose + 1;
				response.append(source, indexOfSrcBodyEnd, nextChar);
				// rewrite(response, source, nextChar);
				// return;
				iOffset = nextChar;
				continue;
			    }
			}
		    }
		} else {
		    iOffset = continueToNextToken(response, source, iOffset, indexOfImgOpen);
		    continue;
		}
	    } else if (indexOfAreaOpen >= 0) {
		if (!isPrefixed(source, indexOfAreaOpen)) {
		    final int indexOfAreaClose = source.indexOf(">", indexOfAreaOpen);
		    if (indexOfAreaClose >= 0) {
			final int indexOfHrefBodyStart = findHrefBodyStart(source, indexOfAreaOpen, indexOfAreaClose);
			if (indexOfHrefBodyStart >= 0) {
			    final char hrefBodyStartChar = source.charAt(indexOfHrefBodyStart - 1);
			    final int indexOfHrefBodyEnd = findHrefBodyEnd(source, indexOfHrefBodyStart, hrefBodyStartChar);
			    if (indexOfHrefBodyEnd >= 0) {
				final int indexOfCardinal = source.indexOf("#", indexOfHrefBodyStart);
				boolean hasCardinal = indexOfCardinal > indexOfHrefBodyStart
					&& indexOfCardinal < indexOfHrefBodyEnd;
				if (hasCardinal) {
				    response.append(source, iOffset, indexOfCardinal);
				} else {
				    response.append(source, iOffset, indexOfHrefBodyEnd);
				}

				final String checksum = calculateChecksum(source, indexOfHrefBodyStart, indexOfHrefBodyEnd);
				final int indexOfQmark = source.indexOf("?", indexOfHrefBodyStart);
				if (indexOfQmark == -1 || indexOfQmark > indexOfHrefBodyEnd) {
				    response.append('?');
				} else {
				    response.append("&amp;");
				}
				response.append(CHECKSUM_ATTRIBUTE_NAME);
				response.append("=");
				response.append(checksum);

				if (hasCardinal) {
				    response.append(source, indexOfCardinal, indexOfHrefBodyEnd);
				}

				final int nextChar = indexOfAreaClose + 1;
				response.append(source, indexOfHrefBodyEnd, nextChar);
				// rewrite(response, source, nextChar);
				// return;
				iOffset = nextChar;
				continue;
			    }
			}
		    }
		} else {
		    iOffset = continueToNextToken(response, source, iOffset, indexOfAreaOpen);
		    continue;
		}
	    }
	    response.append(source, iOffset, source.length());
	    break;
	}

	return response;
    }

    private int min(final int... indexs) {
	int result = Integer.MAX_VALUE;
	for (int i : indexs) {
	    result = Math.min(result, i);
	}
	return result;
    }

    private boolean isRedirectRequest(final HttpServletRequest httpServletRequest) {
	final String uri = httpServletRequest.getRequestURI().substring(FenixWebFramework.getConfig().getAppContext().length() + 1);
	return uri.indexOf("redirect.do") >= 0;
    }

    protected boolean isPrefixed(final StringBuilder source, final int indexOfTagOpen) {
	return (indexOfTagOpen >= LENGTH_OF_NO_CHECKSUM_PREFIX && match(source, indexOfTagOpen - LENGTH_OF_NO_CHECKSUM_PREFIX,
		indexOfTagOpen, NO_CHECKSUM_PREFIX));
    }

    protected boolean match(final StringBuilder source, final int iStart, int iEnd, final String string) {
	if (iEnd - iStart != string.length()) {
	    return false;
	}
	for (int i = 0; i < string.length(); i++) {
	    if (source.charAt(iStart + i) != string.charAt(i)) {
		return false;
	    }
	}
	return true;
    }

    protected int continueToNextToken(final StringBuilder response, final StringBuilder source, final int iOffset,
	    final int indexOfTag) {
	final int nextOffset = indexOfTag + 1;
	response.append(source, iOffset, nextOffset);
	return nextOffset;
    }

}
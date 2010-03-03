package pt.ist.fenixWebFramework.renderers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pt.ist.fenixWebFramework.renderers.components.HtmlComponent;
import pt.ist.fenixWebFramework.renderers.components.HtmlMenu;
import pt.ist.fenixWebFramework.renderers.components.HtmlMenuOption;
import pt.ist.fenixWebFramework.renderers.components.converters.BiDirectionalConverter;
import pt.ist.fenixWebFramework.renderers.components.converters.ConversionException;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixWebFramework.renderers.contexts.PresentationContext;
import pt.ist.fenixWebFramework.renderers.layouts.Layout;
import pt.ist.fenixWebFramework.renderers.model.MetaObject;
import pt.ist.fenixWebFramework.renderers.model.MetaObjectFactory;
import pt.ist.fenixWebFramework.renderers.model.MetaObjectKey;
import pt.ist.fenixWebFramework.renderers.model.MetaSlot;
import pt.ist.fenixWebFramework.renderers.model.MetaSlotKey;
import pt.ist.fenixWebFramework.renderers.schemas.Schema;
import pt.ist.fenixWebFramework.renderers.utils.RenderKit;
import pt.ist.fenixWebFramework.renderers.utils.RenderMode;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;

/**
 * This renderer as a purpose similar to
 * {@link pt.ist.fenixWebFramework.renderers.CheckBoxOptionListRenderer} but is
 * intended to collect only one value. All the possible values for the slot
 * beeing edited are presented in an html menu. The presentation of each object
 * must have in consideration that the object is beeing presented in an option
 * of the menu so it must be short and simple. If possible used the
 * <tt>format</tt> property to format the object. Nevertheless the usual
 * configuration is possible with <tt>eachLayout</tt> and <tt>eachSchema</tt>.
 * 
 * <p>
 * Example: <select> <option>&lt;object A presentation&gt;</option>
 * <option>&lt;object B presentation&gt;</option> <option>&lt;object C
 * presentation&gt;</option> </select>
 * 
 * @author cfgi
 */
public class MenuOptionListRenderer extends InputRenderer {
    private String format;

    private String eachSchema;

    private String eachLayout;

    private String providerClass;

    private DataProvider provider;

    private String sortBy;

    private boolean saveOptions;

    private boolean nullOptionHidden;
    private String defaultText;
    private String bundle;
    private boolean key;

    public String getFormat() {
	return this.format;
    }

    /**
     * This allows to specify a presentation format for each object. For more
     * details about the format syntaxt check the {@see FormatRenderer}.
     * 
     * @property
     */
    public void setFormat(String format) {
	this.format = format;
    }

    public String getEachLayout() {
	return this.eachLayout;
    }

    /**
     * The layout to be used when presenting each object. This property will
     * only be used if {@link #setFormat(String) format} is not specified.
     * 
     * @property
     */
    public void setEachLayout(String eachLayout) {
	this.eachLayout = eachLayout;
    }

    public String getEachSchema() {
	return this.eachSchema;
    }

    /**
     * The schema to be used when presenting each object.
     * 
     * @property
     */
    public void setEachSchema(String eachSchema) {
	this.eachSchema = eachSchema;
    }

    public String getProviderClass() {
	return this.providerClass;
    }

    /**
     * The class name of a {@linkplain DataProvider data provider}. The provider
     * is responsible for constructing a collection will all possible values.
     * 
     * @property
     */
    public void setProviderClass(String providerClass) {
	this.providerClass = providerClass;
    }

    public String getSortBy() {
	return this.sortBy;
    }

    /**
     * With this property you can set the criteria used to sort the collection
     * beeing presented. The accepted syntax for the criteria can be seen in
     * {@link RenderUtils#sortCollectionWithCriteria(Collection, String)}.
     * 
     * @property
     */
    public void setSortBy(String sortBy) {
	this.sortBy = sortBy;
    }

    public String getBundle() {
	return this.bundle;
    }

    /**
     * The bundle used if <code>key</code> is <code>true</code>
     * 
     * @property
     */
    public void setBundle(String bundle) {
	this.bundle = bundle;
    }

    public String getDefaultText() {
	return this.defaultText;
    }

    /**
     * The text or key of the default menu title.
     * 
     * @property
     */
    public void setDefaultText(String defaultText) {
	this.defaultText = defaultText;
    }

    public boolean isKey() {
	return this.key;
    }

    /**
     * Indicates the the default text is a key to a resource bundle.
     * 
     * @property
     */
    public void setKey(boolean key) {
	this.key = key;
    }

    public boolean isSaveOptions() {
	return saveOptions;
    }

    /**
     * Allows the possible object list to be persisted between requests, meaning
     * that the provider is invoked only once.
     * 
     * @property
     */
    public void setSaveOptions(boolean saveOptions) {
	this.saveOptions = saveOptions;
    }

    public boolean isNullOptionHidden() {
	return this.nullOptionHidden;
    }

    /**
     * Don't show the default option, that is, the options meaning no value
     * selected.
     * 
     * @property
     */
    public void setNullOptionHidden(boolean nullOptionHidden) {
	this.nullOptionHidden = nullOptionHidden;
    }

    @Override
    protected Layout getLayout(Object object, Class type) {
	return new MenuOptionLayout();
    }

    protected DataProvider getProvider() {
	if (this.provider == null) {
	    String className = getProviderClass();

	    try {
		Class<?> providerCass = Class.forName(className);
		this.provider = (DataProvider) providerCass.newInstance();
	    } catch (Exception e) {
		throw new RuntimeException("could not get a data provider instance", e);
	    }
	}

	return this.provider;
    }

    protected Converter getConverter() {
	DataProvider provider = getProvider();
	return provider == null ? null : provider.getConverter();
    }

    protected Collection<?> getPossibleObjects() {
	Object object = ((MetaSlot) getInputContext().getMetaObject()).getMetaObject().getObject();
	Object value = getInputContext().getMetaObject().getObject();

	if (getProviderClass() != null) {
	    try {
		DataProvider provider = getProvider();
		Collection<?> collection = (Collection<?>) provider.provide(object, value);

		if (getSortBy() == null) {
		    return collection;
		} else {
		    return RenderUtils.sortCollectionWithCriteria(collection, getSortBy());
		}
	    } catch (Exception e) {
		throw new RuntimeException("exception while executing data provider", e);
	    }
	} else {
	    throw new RuntimeException("a data provider must be supplied");
	}
    }

    class MenuOptionLayout extends Layout {

	@Override
	public HtmlComponent createComponent(Object object, Class type) {
	    HtmlMenu menu = new HtmlMenu();

	    if (!isNullOptionHidden()) {
		String defaultOptionTitle = getDefaultTitle();
		menu.createDefaultOption(defaultOptionTitle).setSelected(object == null);
	    }

	    RenderKit kit = RenderKit.getInstance();
	    Schema schema = kit.findSchema(getEachSchema());

	    List<MetaObject> possibleMetaObjects;

	    if (hasSavedPossibleMetaObjects()) {
		possibleMetaObjects = getPossibleMetaObjects();
	    } else {
		possibleMetaObjects = new ArrayList<MetaObject>();
		for (Object possibility : getPossibleObjects()) {
		    possibleMetaObjects.add(MetaObjectFactory.createObject(possibility, schema));
		}
	    }

	    for (MetaObject metaObject : possibleMetaObjects) {
		Object obj = metaObject.getObject();
		HtmlMenuOption option = menu.createOption(null);

		if (getConverter() instanceof BiDirectionalConverter) {
		    option.setValue(((BiDirectionalConverter) getConverter()).deserialize(obj));
		} else {
		    MetaObjectKey key = metaObject.getKey();
		    option.setValue(key.toString());
		}

		if ((getEachLayout() == null) && (!Enum.class.isAssignableFrom(obj.getClass()))) {
		    option.setText(getObjectLabel(obj));
		} else {
		    PresentationContext newContext = getContext().createSubContext(metaObject);
		    newContext.setLayout(getEachLayout());
		    newContext.setRenderMode(RenderMode.getMode("output"));

		    HtmlComponent component = kit.render(newContext, obj);
		    option.setBody(component);
		}

		if (obj.equals(object)) {
		    option.setSelected(true);
		}
	    }

	    if (isSaveOptions()) {
		savePossibleMetaObjects(possibleMetaObjects);
	    }

	    Converter converter = getConverter();
	    menu.setConverter(new OptionConverter(possibleMetaObjects, converter));

	    menu.setTargetSlot((MetaSlotKey) getInputContext().getMetaObject().getKey());
	    return menu;
	}

	private boolean hasSavedPossibleMetaObjects() {
	    return getInputContext().getViewState().getLocalAttribute("options") != null;
	}

	private List<MetaObject> getPossibleMetaObjects() {
	    return (List<MetaObject>) getInputContext().getViewState().getLocalAttribute("options");
	}

	private void savePossibleMetaObjects(List<MetaObject> possibleMetaObjects) {
	    getInputContext().getViewState().setLocalAttribute("options", possibleMetaObjects);
	}

	// TODO: duplicate code, id=menu.getDefaultTitle
	private String getDefaultTitle() {
	    if (getDefaultText() == null) {
		return RenderUtils.getResourceString("renderers.menu.default.title");
	    } else {
		if (isKey()) {
		    return RenderUtils.getResourceString(getBundle(), getDefaultText());
		} else {
		    return getDefaultText();
		}
	    }
	}

	protected String getObjectLabel(Object object) {
	    if (getFormat() != null) {
		return RenderUtils.getFormattedProperties(getFormat(), object);
	    } else {
		return String.valueOf(object);
	    }
	}

    }

    private static class OptionConverter extends Converter {

	private final List<MetaObject> metaObjects;
	private final Converter converter;

	public OptionConverter(List<MetaObject> metaObjects, Converter converter) {
	    this.metaObjects = metaObjects;
	    this.converter = converter;
	}

	@Override
	public Object convert(Class type, Object value) {
	    String textValue = (String) value;

	    if (textValue == null || textValue.length() == 0) {
		return null;
	    } else {
		if (this.converter != null) {
		    return this.converter.convert(type, value);
		} else {
		    for (MetaObject metaObject : this.metaObjects) {
			if (textValue.equals(metaObject.getKey().toString())) {
			    return metaObject.getObject();
			}
		    }

		    throw new ConversionException("renderers.menuOption.convert.invalid.value");
		}
	    }
	}

    }
}

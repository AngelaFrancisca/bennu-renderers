package pt.ist.fenixWebFramework.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import pt.utl.ist.fenix.tools.util.FileUtils;

public class ServiceAnnotationInjector {

    private static final String SERVICE_MANAGER_PACKAGE = ServiceManager.class.getPackage().getName();
    private static final String SERVICE_MANAGER_NAME = ServiceManager.class.getSimpleName();


    public static void main(String[] args) {
	final ClassPool classPool = ClassPool.getDefault();
	classPool.appendSystemPath();
	try {
	    for (final String arg : args) {
		classPool.appendClassPath(arg);
	    }
	} catch (NotFoundException e) {
	    throw new Error(e);
	}

	classPool.importPackage(SERVICE_MANAGER_PACKAGE);
	classPool.importPackage("pt.ist.fenixWebFramework.security");
	classPool.importPackage("pt.ist.fenixframework.pstm");
	classPool.importPackage("pt.ist.fenixWebFramework._development");

	File file = null;
	try {
	    file = new File(ServiceAnnotationProcessor.LOG_FILENAME);
	    if (file.exists()) {
		final String fileContents = FileUtils.readFile(new FileInputStream(file));
		final String[] lines = fileContents.split(ServiceAnnotationProcessor.ENTRY_SEPERATOR);
		for (final String line : lines) {
		    final String[] strings = line.split(ServiceAnnotationProcessor.FIELD_SEPERATOR);
		    process(args[0], classPool, strings[0], strings[1]);
		}
	    }
	} catch (FileNotFoundException e) {
	    throw new Error(e);
	} catch (IOException e) {
	    throw new Error(e);
	} finally {
	    if (file != null && file.exists()) {
		file.delete();
	    }
	}
    }

    private static void process(final String outputFolder, final ClassPool classPool, final String className, final String methodName) {
	try {
	    CtClass classToInject = classPool.get(className);

	    for (final CtMethod ctMethod : classToInject.getDeclaredMethods()) {
		if (ctMethod.getName().equals(methodName)) {
		    ctMethod.setName("_" + methodName + "_");

		    final CtMethod newCtMethod = new CtMethod(ctMethod.getReturnType(), methodName, ctMethod.getParameterTypes(), ctMethod.getDeclaringClass());
		    newCtMethod.setModifiers(ctMethod.getModifiers());
		    final String body = getWrapperMethod(ctMethod, className, methodName);
		    newCtMethod.setBody(body);
		    classToInject.addMethod(newCtMethod);
		}
	    }
	    classToInject.writeFile(outputFolder);
	    classToInject.detach();
	} catch (NotFoundException e) {
	    throw new Error(e);
	} catch (CannotCompileException e) {
	    throw new Error(e);
	} catch (IOException e) {
	    throw new Error(e);
	}
    }

    private static CtMethod findCtMethid(final CtClass ctClass, final String methodName) {
	for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {
	    if (ctMethod.getName().equals(methodName)) {
		return ctMethod;
	    }
	}
	return null;
    }

    private static String getWrapperMethod(final CtMethod ctMethod, final String className, final String methodName) throws NotFoundException {
	final CtClass returnType = ctMethod.getReturnType();
	final boolean isPrimitive = returnType.isPrimitive();
	final boolean isVoid = isPrimitive && returnType.getSimpleName().equals("void");
	final String serviceName = className + "." + methodName;

	final StringBuilder stringBuilder = new StringBuilder();
	stringBuilder.append("{\n");
		
	if (!isVoid) {
	    stringBuilder.append("\t");
	    stringBuilder.append(returnType.getSimpleName());
	    stringBuilder.append(" _result_ = null;\n");
	}

	stringBuilder.append("if (ServiceManager.isInsideService()) {");
	appendSeriveInvocation(stringBuilder, isVoid, returnType, ctMethod);
	stringBuilder.append("} else {");
	stringBuilder.append("    ServiceManager.enterService();");
	stringBuilder.append("    try {");

	stringBuilder.append("final User user = UserView.getUser();");
	stringBuilder.append("final String username = user == null ? null : user.getUsername();");
	stringBuilder.append("ServiceInfo.setCurrentServiceInfo(username, \"");
	stringBuilder.append(serviceName);
	stringBuilder.append("\", $args);");

	stringBuilder.append("Transaction.setDefaultReadOnly(!ServiceManager.KNOWN_WRITE_SERVICES.containsKey(\"");
	stringBuilder.append(serviceName);
	stringBuilder.append("\"));");

	stringBuilder.append("boolean keepGoing = true;");
	stringBuilder.append("int tries = 0;");
	stringBuilder.append("try {");
	stringBuilder.append("    while (keepGoing) {");
	stringBuilder.append("	tries++;");
	stringBuilder.append("	try {");
	appendSeriveInvocation(stringBuilder, isVoid, returnType, ctMethod);
	stringBuilder.append("	    keepGoing = false;");
	stringBuilder.append("	} catch (jvstm.CommitException commitException) {");
//	stringBuilder.append("	    logTransactionRestart(commitException, tries);");
	stringBuilder.append("	} catch (IllegalWriteException illegalWriteException) {");
	stringBuilder.append("	    ServiceManager.KNOWN_WRITE_SERVICES.put(\"");
	stringBuilder.append(serviceName);
	stringBuilder.append("\", \"");
	stringBuilder.append(serviceName);
	stringBuilder.append("\");");
	stringBuilder.append("	    Transaction.setDefaultReadOnly(false);");
//	stringBuilder.append("	    logTransactionRestart(illegalWriteException, tries);");
	stringBuilder.append("	}");
	stringBuilder.append("    }");
	stringBuilder.append("} finally {");
	stringBuilder.append("    Transaction.setDefaultReadOnly(false);");
	stringBuilder.append("    if (LogLevel.INFO) {");
	stringBuilder.append("	if (tries > 1) {");
	stringBuilder.append("	    System.out.println(\"Service ");
	stringBuilder.append(serviceName);
	stringBuilder.append(" took \" + tries + \" tries.\");");
	stringBuilder.append("	}");
	stringBuilder.append("    }");
	stringBuilder.append("}");

	stringBuilder.append("    } finally {");
	stringBuilder.append("	ServiceManager.exitService();");
	stringBuilder.append("    }");
	stringBuilder.append("}");

	if (!isVoid) {
	    stringBuilder.append("\treturn _result_;\n");
	}
	stringBuilder.append("}");
	return stringBuilder.toString();
    }

    private static void appendSeriveInvocation(final StringBuilder stringBuilder, final boolean isVoid, final CtClass returnType, final CtMethod ctMethod) {
	if (!isVoid) {
	    stringBuilder.append("_result_ = ");
	}
	stringBuilder.append(ctMethod.getName());
	stringBuilder.append("($$);\n");
    }

}

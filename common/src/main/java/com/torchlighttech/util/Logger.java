package com.torchlighttech.util;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

public class Logger {

    private static final LoggerSecurityManager LOGGER_SECURITY_MANAGER = new LoggerSecurityManager();
    private static final boolean IS_LOGGING_ON = true;
    private static final String TAG_TEXT = "+ VidAngel ";
    public enum Priority{
        ERROR,
        WARN,
        DEBUG,
        INFO,
        VERBOSE
    }

    public static void log(Priority priority, String className, String methodName, String message) {
        String tag = className + "." + methodName;
        Logger.log(priority, tag, message);
    }

    public static void log(Priority priority, String tag, String message) {
        switch (priority) {
            case ERROR: {
                Logger.e(tag, message);
                break;
            }
            case WARN: {
                Logger.w(tag, message);
                break;
            }
            case DEBUG: {
                Logger.d(tag, message);
                break;
            }
            case INFO: {
                Logger.i(tag, message);
                break;
            }
            case VERBOSE: {
            }
            default: {
                Logger.v(tag, message);
                break;
            }

        }
    }

    public static void log(Priority priority, String className, String methodName, String message, Throwable throwable) {
        String tag = className + "." + methodName;
        Logger.log(priority, tag, message, throwable);
    }

    public static void log(Priority priority, String tag, String message, Throwable throwable) {
        switch (priority) {
            case ERROR: {
                Logger.e(tag, message, throwable);
                break;
            }
            case WARN: {
                Logger.w(tag, message, throwable);
                break;
            }
            case DEBUG: {
                Logger.d(tag, message, throwable);
                break;
            }
            case INFO: {
                Logger.i(tag, message, throwable);
                break;
            }
            case VERBOSE: {
            }
            default: {
                Logger.v(tag, message, throwable);
                break;
            }
        }
    }

    /*public static void println(Priority priority, String tag, String message) {
        if (IS_LOGGING_ON) {
            Log.println(priority, TAG_TEXT + tag, message);
        }
    }*/

    /*public static boolean isLoggable(String tag, int level) {
        return Log.isLoggable(tag, level);
    }*/


    public static String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString(); // stack trace as a string
    }

    public static void printStackTraceString(Priority priority, String tag, Throwable throwable) {
        if (IS_LOGGING_ON) {
            log(priority, tag, getStackTraceString(throwable));
        }
    }

    private static void log(Priority priority, String defaultTag, Object... params) {
        String tag = defaultTag;
        String delim = "{}";
        int i = 0;
        if (params.length >= 2 && params[0] instanceof String && params[1] instanceof String && ((String) params[0]).indexOf(delim) == -1) {
            tag = (String) params[0];
            i++;
        }
        StringBuilder msg = new StringBuilder();
        for (; i < params.length; i++) {
            if (params[i] instanceof Throwable) {
                msg.append(getStackTraceString((Throwable) params[i]));
            } else if (params[i] instanceof String) {
                String param = (String) params[i];

                if (param.indexOf(delim) > 0) {
                    StringBuilder sb = new StringBuilder();
                    while (param.indexOf(delim) > 0 && i < params.length - 1) {
                        int idx = param.indexOf(delim);
                        sb.append(param.substring(0, idx))
                                .append(params[++i]);
                        param = param.substring(idx + 2);
                    }
                    msg.append(sb.toString())
                            .append(param);
                } else {
                    msg.append(param);
                }
            } else {
                msg.append(params[i]);
            }
        }
        String[] messages = msg.toString().split("\n");
        msg = new StringBuilder();
        int logcatLimit = 8000;
        int num = 0;
        for (i = 0; i < messages.length; i++) {
            if (msg.length() + messages[i].length() <= logcatLimit && num > 0) {
                msg.append(messages[i]);
            } else {
                java.util.logging.Logger.getLogger(TAG_TEXT + tag).log(getLevel(priority), msg.toString());
//                Log.println(priority, TAG_TEXT + tag, msg.toString());
                msg = new StringBuilder(messages[i]);
                num = 0;
            }
        }
        if (msg.length() > 0) {
//            Log.println(priority, TAG_TEXT + tag, msg.toString());
            java.util.logging.Logger.getLogger(TAG_TEXT + tag).log(getLevel(priority), msg.toString());

        }
    }

    private static Level getLevel(Priority priority) {
        switch (priority) {
            case DEBUG:
                return Level.INFO;
            case ERROR:
                return Level.SEVERE;
            case INFO:
                return Level.INFO;
            case VERBOSE:
                return Level.FINE;
            case WARN:
                return Level.WARNING;
        }
        return Level.INFO;
    }

    public static void e(Object... params) {
        if (IS_LOGGING_ON) {
            log(Priority.ERROR, LOGGER_SECURITY_MANAGER.getCallerClassName(), params);
        }
    }

    public static void w(Object... params) {
        if (IS_LOGGING_ON) {
            log(Priority.WARN, LOGGER_SECURITY_MANAGER.getCallerClassName(), params);
        }
    }

    public static void i(Object... params) {
        if (IS_LOGGING_ON) {
            log(Priority.INFO, LOGGER_SECURITY_MANAGER.getCallerClassName(), params);
        }
    }

    public static void d(Object... params) {
        if (IS_LOGGING_ON) {
            log(Priority.DEBUG, LOGGER_SECURITY_MANAGER.getCallerClassName(), params);
        }
    }

    public static void v(Object... params) {
        if (IS_LOGGING_ON) {
            log(Priority.VERBOSE, LOGGER_SECURITY_MANAGER.getCallerClassName(), params);
        }
    }

    public static void mark() {
        if (IS_LOGGING_ON) {
            log(Priority.VERBOSE, LOGGER_SECURITY_MANAGER.getCallerClassName(), LOGGER_SECURITY_MANAGER.getCallerClassAndMethodName());
        }
    }

    public static void mark(Priority priority) {
        if (IS_LOGGING_ON) {
            log(priority, LOGGER_SECURITY_MANAGER.getCallerClassName(), LOGGER_SECURITY_MANAGER.getCallerClassAndMethodName());
        }
    }

    static class LoggerSecurityManager extends SecurityManager {
        public String getCallerClassName() {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();

            if (null == trace) {
                return "";
            }

            try {
                return ClassUtils.getSimpleClassNameForClass(getClassContext()[2], false);
            } catch (Exception e) {
                try {
                    StackTraceElement[] ste = Thread.currentThread().getStackTrace();
                    String cn = ste[4].getClassName();
                    return cn.substring(cn.lastIndexOf('.') + 1);
                } catch (Exception ee) {
                    return "Class unknown.";
                }
            }
        }

        public String getCallerClassAndMethodName() {
            String st = null;
            try {
                throw new Exception();
            } catch (Exception e) {
                try {
                    st = getStackTraceString(e);
                    String[] lines = st.split("\n");
                    lines = lines[3].split(" ");
                    return lines[1];
                } catch (Exception ee) {
                    Logger.e(e);
                }
            }
            return st;
        }
    }

    private static class JsonLogMessage {
        public String prefix;
        public String object;
        public String postfix;
    }
}

package com.sekift.logger.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sekift.logger.IProcessUnit;
import com.sekift.logger.enums.LogFrameworkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.sekift.logger.constant.LogConstant.*;

/**
 * 日志调整抽象类
 * 支持logback
 *
 * @author sekift
 * @date 2018-04-27
 */
public class LogbackProcessUnitImpl implements IProcessUnit {

    private Logger log = LoggerFactory.getLogger(LogbackProcessUnitImpl.class);

    private final LogFrameworkType logFrameworkType;

    private final ConcurrentHashMap<String, Object> loggerMap = new ConcurrentHashMap<>();

    private static IProcessUnit instance = new LogbackProcessUnitImpl();

    public static IProcessUnit getSingleton() {
        return instance;
    }

    public LogbackProcessUnitImpl() {
        log.info("[LoggerLevel]start");
        String type = StaticLoggerBinder.getSingleton().getLoggerFactoryClassStr();
        if (log.isDebugEnabled()) {
            log.debug("[LoggerLevel]log type={}", type);
        }
        if (LOGBACK_LOGGER_FACTORY.equals(type)) {
            logFrameworkType = LogFrameworkType.LOGBACK;
            ch.qos.logback.classic.LoggerContext loggerContext = (ch.qos.logback.classic.LoggerContext) LoggerFactory.getILoggerFactory();
            for (ch.qos.logback.classic.Logger logger : loggerContext.getLoggerList()) {
                if (logger.getLevel() != null) {
                    loggerMap.put(logger.getName(), logger);
                }
            }
            ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            loggerMap.put(rootLogger.getName(), rootLogger);
        } else {
            logFrameworkType = LogFrameworkType.UNKNOWN;
            log.error("[LoggerLevel]Log框架无法识别:type={}", type);
            return;
        }
        log.info("[LoggerLevel]loggerMap={}", loggerMap);
        this.getLoggerList();
    }

    @Override
    public String setLogLevel(String logLevel) {
        log.info("[LoggerLevel]设置所有Log级别为[{}]", logLevel);
        if (null == loggerMap || loggerMap.isEmpty()) {
            log.warn("[LoggerLevel]当前工程中不存在任何Logger,无法调整Logger级别");
            return "";
        }
        Set<Map.Entry<String, Object>> entries = loggerMap.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            Object logger = entry.getValue();
            if (null == logger) {
                throw new RuntimeException(LOGGER_NOT_EXSIT);
            }
            if (logFrameworkType == LogFrameworkType.LOGBACK) {
                ch.qos.logback.classic.Logger targetLogger = (ch.qos.logback.classic.Logger) logger;
                ch.qos.logback.classic.Level targetLevel = ch.qos.logback.classic.Level.toLevel(logLevel);
                targetLogger.setLevel(targetLevel);
            } else {
                throw new RuntimeException(LOGGER_TYPE_UNKNOWN);
            }
        }
        return "success";
    }

    @Override
    public String setLogLevel(String loggerName, String loggerLevel) {
        log.info("setLogLevel: loggerName = {}, loggerLevel = {}", loggerName, loggerLevel);
        Object logger = loggerMap.get(loggerName);
        if (null == logger) {
            throw new RuntimeException(LOGGER_NOT_EXSIT);
        }
        if (logFrameworkType == LogFrameworkType.LOGBACK) {
            ch.qos.logback.classic.Logger targetLogger = (ch.qos.logback.classic.Logger) logger;
            ch.qos.logback.classic.Level targetLevel = ch.qos.logback.classic.Level.toLevel(loggerLevel);
            targetLogger.setLevel(targetLevel);
        } else {
            throw new RuntimeException(LOGGER_TYPE_UNKNOWN);
        }
        return "success";
    }

    @Override
    public String getLoggerList() {
        JSONObject result = new JSONObject();
        result.put(LOG_FRAMEWORK, logFrameworkType);
        JSONArray loggerList = new JSONArray();
        for (ConcurrentMap.Entry<String, Object> entry : loggerMap.entrySet()) {
            JSONObject loggerJSON = new JSONObject();
            loggerJSON.put(LOGGER_NAME, entry.getKey());
            if (logFrameworkType == LogFrameworkType.LOGBACK) {
                ch.qos.logback.classic.Logger targetLogger = (ch.qos.logback.classic.Logger) entry.getValue();
                loggerJSON.put(LOGGER_LEVEL, targetLogger.getLevel().toString());
            } else {
                loggerJSON.put(LOGGER_LEVEL, LOGGER_TYPE_UNKNOWN);
            }
            loggerList.add(loggerJSON);
        }
        result.put(LOGGER_LIST, loggerList);
        log.info("result = {}", result);
        return result.toString();
    }

}
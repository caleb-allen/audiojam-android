# .SUFFIXES: .java .m
# .SUFFIXES: .java .m

BUILD_DIR = objc_build
SOURCE_DIR = src/main/java
# JAR_NAMES = :$(LIBS)gson-2.8.0.jar
LIBS = source_libs/
JAR_NAMES = :$(LIBS)gson-2.8.0.jar:$(LIBS)retrofit-1.9.0-doppl.jar:$(LIBS)rxjava-1.3.0.jar:$(LIBS)JTransforms-3.1.jar:$(LIBS)JLargeArrays-1.6-NEW.jar:$(LIBS)commons-math3-3.5.jar

# Change to where distribution was unzipped.
J2OBJC_DISTRIBUTION = /Users/caleb/Android/j2objc-2.0
J2OBJC = $(J2OBJC_DISTRIBUTION)/j2objc
J2OBJCC = $(J2OBJC_DISTRIBUTION)/j2objcc

CC = $(J2OBJCC)

# OBJECTS = \
#   $(BUILD_DIR)/com/torchlighttech/data/effects/Strobe.o \
#   $(BUILD_DIR)/com/torchlighttech/data/effects/Flash.o
# JAVA_OBJECTS = $(SOURCE_DIR)/com/torchlighttech/data/effects/Strobe.java
# JAVA_OBJECTS = $(SOURCE_DIR)/com/torchlighttech/audio/AudioUtil.java
# JAVA_OBJECTS = $(SOURCE_DIR)/com/torchlighttech/data/gson/SGson.java

JAVA_OBJECTS := $(shell find $(SOURCE_DIR) -name '*.java')
# JAVA_OBJECTS := $(shell find $(SOURCE_DIR) -name '*.java' -o -name '*.jar')
OBJECTS_A = $(JAVA_OBJECTS:.java=.o)
OBJECTS = $(OBJECTS_A:$(SOURCE_DIR)%=$(BUILD_DIR)%)

OBJC_SOURCES = $(OBJECTS:.o=.m)
# RESULT = mainclass

# $(info OBJECTS is $(OBJECTS))

default: translate $(OBJECTS)

# $(J2OBJCC) -o $(RESULT) $(OBJECTS)

translate: $(BUILD_DIR) $(OBJC_SOURCES)


$(BUILD_DIR)/%.m $(BUILD_DIR)/%.h: $(SOURCE_DIR)/%.java
	$(J2OBJC) -sourcepath "$(SOURCE_DIR)$(JAR_NAMES)" -d $(BUILD_DIR) --build-closure -Xuse-jdt $?
 # --doc-comments -use-arc
# 	$(J2OBJC) -sourcepath $(SOURCE_DIR) -classpath "$(CLASS_PATH)" -d $(BUILD_DIR) --build-closure $?

$(BUILD_DIR)/%.o: $(BUILD_DIR)/%.m
	$(J2OBJCC) -I$(BUILD_DIR) -c $? -o $@

$(BUILD_DIR):
	@mkdir $(BUILD_DIR)

clean:
	@rm -rf $(BUILD_DIR)


# OBJECTS := $(shell find $(SOURCE_DIR) -name '*.o')

# ~/Android/j2objc-dist/j2objc -sourcepath src/main/java/ -classpath '$(LIBS)gson-2.8.0.jar' -d build src/main/java/com/torchlighttech/data/effects/Strobe.java

# BUILD_DIR = objc_build
# SOURCE_DIR = src
# # SOURCE_DIR = src/main/java/com/torchlighttech

# # Change to where distribution was unzipped.
# J2OBJC_DISTRIBUTION = /Users/caleb/Android/j2objc-dist

# J2OBJC = $(J2OBJC_DISTRIBUTION)/j2objc
# J2OBJCC = $(J2OBJC_DISTRIBUTION)/j2objcc
# OBJECTS = \
#   $(SOURCE_DIR)/main/java/com/torchlighttech/api/TorchlightApiClient.java
#   # $(BUILD_DIR)/foo/bar/Support.o \
#   # $(BUILD_DIR)/foo/bar/Utils.o
# OBJC_SOURCES = $(OBJECTS:.o=.m)
# RESULT = mainclass

# default: translate $(OBJECTS)
# 	$(J2OBJCC) -o $(RESULT) $(OBJECTS)

# translate: pre_translate $(OBJC_SOURCES)
# 	@if [ `cat $(JAVA_SOURCES) | wc -l` -ge 1 ] ; then \
# 	  $(J2OBJC) -sourcepath $(SOURCE_DIR) -d $(BUILD_DIR) \
# 	    `cat $(JAVA_SOURCES)` ; \
# 	fi
# clean:
# 	@rm -rf $(RESULT) $(BUILD_DIR)

# $(BUILD_DIR)/%.m $(BUILD_DIR)/%.h: $(SOURCE_DIR)/%.java
# 	@echo $? >> $(JAVA_SOURCES)

# $(BUILD_DIR)/%.o: $(BUILD_DIR)/%.m
# 	$(J2OBJCC) -I$(BUILD_DIR) -c $? -o $@

# $(BUILD_DIR):
# 	@mkdir $(BUILD_DIR)

# JAVA_SOURCES = $(TMPDIR).sources.list

# pre_translate: $(BUILD_DIR)
# 	@rm -f $(JAVA_SOURCES)
# 	@touch $(JAVA_SOURCES)

# # MY_OBJS = $(shell find $(SOURCE_ROOTS) -name '*.java' | sed s/\java$/o/)

# # pre_translate:
# #         @rm -f $(JAVA_SOURCE_LIST)
# #         @touch $(JAVA_SOURCE_LIST)


# # translate: pre_translate $(JAVA_SOURCES) $(FRAMEWORK_OBJS:.o=.m) $(INTERNAL_OBJS:.o=.m)
# #         @if [ `cat $(JAVA_SOURCE_LIST) | wc -l` -ge 1 ] ; then \
# #           $(J2OBJC) -sourcepath .:$(SOURCE_BASE)/java `cat $(JAVA_SOURCE_LIST)` ; \
# #         fi
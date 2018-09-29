# A mostly generic makefile that assumes there is one or more app jars in
#
#    ./target
#
# and 0 or more library jars
#
#    ./target/lib
#
# Since mave does most of the work, you're pom.xml will have to configure
# the maven-dependency-plugin to put the needed jars into ./target/lib


# Generic definitions that have to be formed in a platform specific way:

ifeq ($(H_ARCH),)
JAVAVER:=$(shell javac platver.java && java -cp . platver 2>/dev/null)
ifeq ($(JAVAVER),)
$(error Couldn't determine your java platform version)
endif
H_ARCH=java$(JAVAVER)
endif

ifeq ($(INST_HOST_LIB),)
INST_HOST_LIB=$(PREFIX)/lib/$(H_ARCH)
endif
export INST_HOST_LIB

ifeq ($(INST_EXT_LIB),)
INST_EXT_LIB=$(PREFIX)/lib/$(N_ARCH)/$(H_ARCH)
endif

# Project Targets ############################################################

LIB_JAR=dasCore-2.2.jar

DEPEND_JARS=batik-awt-util-1.5.jar batik-svggen-1.5.jar \
 batik-util-1.5.jar bcmail-jdk14-138.jar bcprov-jdk14-138.jar \
 itext-2.0.7.jar  junit-3.8.1.jar  swing-layout-1.0.3.jar
 
#SCRIPTS=das2_runrdr

# Targets with autopaths #####################################################

BUILD_JAR=$(patsubst %.jar, target/%.jar, $(LIB_JAR))

#BUILD_SCRIPTS=$(patsubst %, target/%, $(SCRIPTS))

BUILD_DEPEND_JARS=$(patsubst %.jar, target/lib/%.jar, $(DEPEND_JARS))

INST_LIB_JARS=$(patsubst %.jar, $(INST_HOST_LIB)/%.jar, $(LIB_JAR)) \
  $(patsubst %.jar, $(INST_HOST_LIB)/%.jar, $(DEPEND_JARS))
 
#INST_SCRIPTS=$(patsubst %, $(INST_NAT_BIN)/%, $(SCRIPTS))

# Implicit Rules #############################################################


# Building a bash script
#target/%:src/main/sh/%.in
#	./envsubst.py $< $@
	
# Installing a script
#$(INST_NAT_BIN)/%:target/%
#	install -D -m 775 $< $@

# Installing libary jar
$(INST_HOST_LIB)/%.jar:target/%.jar
	install -D -m 0664 $< $@

# Installing dependency libaries
$(INST_HOST_LIB)/%.jar:target/lib/%.jar
	install -D -m 0664 $< $@
	


# Explicit rules #############################################################

# Maven is a development tool, not a deployment tool, hence it is being run
# by make as if it were just a compliler tool.  Then install steps are handled
# here

.PHONY : build package test install clean distclean

build: $(BUILD_JAR) $(BUILD_DEPEND_JARS) $(BUILD_SCRIPTS) \
 target/site/apidocs/index.html

$(BUILD_JAR) $(BUILD_DEPEND_JARS):
	mvn -Dmaven.javadoc.skip=true package

target/site/apidocs/index.html:
	mvn generate-sources javadoc:javadoc

test:
	mvn integration-test
	
show:
	@echo BUILD_JARS: $(BUILD_JAR) $(BUILD_DEPEND_JARS)
	@echo INST_LIB_JARS: $(INST_LIB_JARS)


install: $(INST_LIB_JARS) $(INST_SCRIPTS)

$(INST_DOC)/dasCore/index.html:target/site/apidocs/index.html
	if [ ! -e $(INST_DOC)/dasCore ]; then mkdir -p $(INST_DOC)/dasCore; fi
	cp --remove-destination -r target/site/apidocs/* $(INST_DOC)/dasCore
	chmod -R g+w $(INST_DOC)/dasCore

clean:
	mvn clean


# Need to get *.java file dependencies in here...

distclean:
	rm -r target
       
	
# maven build phases are (from apach.org):
#
# validate - validate the project is correct and all necessary information 
#            is available
#
# compile  - compile the source code of the project
#
# test     - test the compiled source code using a suitable unit testing
#            framework.  These tests should not require the code be packaged or
#            deployed
# 
# package  - take the compiled code and package it in its distributable format,
#            such as a JAR.
#
# integration-test - process and deploy the package if necessary into an
#            environment where integration tests can be run
#
# verify   - run any checks to verify the package is valid and meets quality
#            criteria
#
# install  - install the package into the local repository, for use as a
#            dependency in other projects locally
#
# deploy - done in an integration or release environment, copies the final
#          package to the remote repository for sharing with other developers
#          and projects.

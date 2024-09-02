<?xml version="1.0" encoding="UTF-8"?>
<exe4j version="6.0.2" transformSequenceNumber="2">
    <directoryPresets config=""/>
    <application name="${applicationName}" distributionSourceDir="${distributionSourceDir}">
        <languages>
            <principalLanguage id="en" customLocalizationFile=""/>
        </languages>
    </application>
    <executable name="${applicationName}" type="2" iconSet="true" iconFile="${iconFile}" executableDir="." redirectStderr="false" stderrFile="error.log" stderrMode="overwrite" redirectStdout="false" stdoutFile="output.log" stdoutMode="overwrite" failOnStderrOutput="true" executableMode="2" changeWorkingDirectory="true" workingDirectory="."
                singleInstance="true" serviceStartType="2" serviceDependencies="" serviceDescription="" jreLocation="" executionLevel="asInvoker" checkConsoleParameter="false" globalSingleInstance="false" singleInstanceActivate="true" dpiAware="java9+" amd64="true">
        <messageSet/>
        <versionInfo include="false" fileVersion="" fileDescription="" legalCopyright="" internalName="" productName="" companyName="" productVersion=""/>
    </executable>
    <splashScreen show="false" width="0" height="0" bitmapFile="" textOverlay="false">
        <text>
            <statusLine x="20" y="20" text="" fontSize="8" fontColor="0,0,0" bold="false"/>
            <versionLine x="20" y="40" text="version %VERSION%" fontSize="8" fontColor="0,0,0" bold="false"/>
        </text>
    </splashScreen>
    <java mainClass="org.springframework.boot.loader.JarLauncher" mainMode="1" vmParameters="-Dfile.encoding=UTF-8" arguments="" allowVMPassthroughParameters="true" preferredVM="client" bundleRuntime="true" minVersion="1.8" maxVersion="" allowBetaVM="false" jdkOnly="false">
        <searchSequence>
            <directory location="./jre"/>
        </searchSequence>
        <classPath>
            <archive location="${classPathArchiveLocation}" failOnError="false"/>
        </classPath>
        <modulePath/>
        <nativeLibraryDirectories/>
        <vmOptions/>
    </java>
    <includedFiles/>
    <unextractableFiles/>
</exe4j>

; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "${applicationName}"
#define MyAppVersion "1.0"
#define MyAppPublisher "windy"
#define MyAppURL "gust.cafe"
#define MyAppExeName "windy-crypto.exe"
#define MyJreDirName "jre"
#define MyAttachmentsDirName "attachments"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{257A33E0-8CE6-43C7-B4C2-AD8A091683A3}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\{#MyAppName}
DisableProgramGroupPage=yes
LicenseFile=D:\gust\dev\project\github\windy-crypto\LICENSE
OutputDir=D:\gust\dev\project\github\windy-crypto\target\exe-exe2setup
OutputBaseFilename=windy-crypto-setup
SetupIconFile=C:\cafe\attachment\ico\53.ico
Compression=lzma
SolidCompression=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "D:\gust\dev\project\github\windy-crypto\target\exe-jar2exe\windy-crypto.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\cafe\dev\jdk1.8.0_221\jre\*"; DestDir: "{app}\{#MyJreDirName}"; Flags: ignoreversion recursesubdirs createallsubdirs
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{commonprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent


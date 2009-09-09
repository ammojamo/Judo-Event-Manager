; Java Launcher
;--------------

Name "Event Manager"
Caption "Event Manager"
;Icon "YourProgram.ico"
OutFile "EventManagerSetup.exe"

InstallDir $PROGRAMFILES\EventManager

DirText "This will install EventManager on your computer."

;You want to change the next two lines too
;!define CLASSPATH ".;lib"
;!define CLASS "au.com.jwatmuff.eventmanager.Main"

Section ""
  SetOutPath $INSTDIR

  File /r ..\..\resources
  File /r ..\..\dist\*.*
  File ..\..\license.lic

  createShortCut "$SMPROGRAMS\EventManager.lnk" "$INSTDIR\EventManager.jar"

SectionEnd

Section "Install Java"

  MessageBox MB_YESNO \
    "EventManager requires Java version 6 or later to be installed on your computer. Do you wish to install Java now?" \
     IDYES installjava

  Goto installjavadone

  installjava:

  File ..\thirdparty\jre-6u16-windows-i586-s.exe
  ExecWait "$INSTDIR\jre-6u16-windows-i586-s.exe"

  installjavadone:

SectionEnd

Section "Install Bonjour"

  MessageBox MB_YESNO \
    "EventManager requires Bonjour to be installed on your computer. Do you wish to install Bonjour now?"\
     IDYES installbonjour

  Goto installbonjourdone

  installbonjour:

  File ..\thirdparty\BonjourSetup.exe
  ExecWait "$INSTDIR\BonjourSetup.exe"

  installbonjourdone:

SectionEnd


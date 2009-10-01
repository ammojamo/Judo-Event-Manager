Name "Event Manager"
Caption "Event Manager"
;Icon "YourProgram.ico"
OutFile "EventManagerSetup.exe"

InstallDir $PROGRAMFILES\EventManager

DirText "This will install EventManager on your computer."

Section ""
  SetOutPath $INSTDIR

;  SetShellVarContext all

  File /r ..\..\resources
  File /r ..\..\dist\*.*
  File ..\..\license.lic
  File icon.ico

  writeUninstaller "$INSTDIR\uninstall.exe"

  CreateDirectory "$SMPROGRAMS\EventManager"
  createShortCut "$SMPROGRAMS\EventManager\EventManager.lnk" "$INSTDIR\EventManager.jar" "" "$INSTDIR\icon.ico"

  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager" \
                   "DisplayName" "EventManager"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager" \
                   "UninstallString" "$\"$INSTDIR\uninstall.exe$\""

SectionEnd

UninstallText "This will uninstall EventManager."

Section "Uninstall"
; try user dir as well as all user dirs
  RMDir /r "$SMPROGRAMS\EventManager"
  SetShellVarContext all
  RMDir /r "$SMPROGRAMS\EventManager"

  RMDir /r "$INSTDIR"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager"
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

  Delete "$INSTDIR\jre-6u16-windows-i586-s.exe"
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

  Delete "$INSTDIR\BonjourSetup.exe"
SectionEnd

Name "Event Manager 2013"
Caption "Event Manager 2013 Beta"
;Icon "YourProgram.ico"
OutFile "EventManagerSetup-2013.exe"

LicenseData eula.txt
LicenseForceSelection checkbox

InstallDir $PROGRAMFILES\EventManager2013

DirText "This will install EventManager 2013 Beta on your computer."

Section ""
  SetOutPath $INSTDIR

;  SetShellVarContext all

  File /r ..\..\resources
  File /r ..\..\dist\*.*
  File ..\..\license.lic
  File icon.ico

  writeUninstaller "$INSTDIR\uninstall.exe"

  CreateDirectory "$SMPROGRAMS\EventManager2013"
  createShortCut "$SMPROGRAMS\EventManager2013\EventManager.lnk" "$INSTDIR\EventManager.jar" "" "$INSTDIR\icon.ico"
  createShortCut "$SMPROGRAMS\EventManager2013\Uninstall.lnk" "$INSTDIR\uninstall.exe"

  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager2013" \
                   "DisplayName" "EventManager"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager2013" \
                   "UninstallString" "$\"$INSTDIR\uninstall.exe$\""

SectionEnd

Page license
Page directory
Page instfiles

UninstallText "This will uninstall EventManager 2013 Beta."

Section "Uninstall"
; try user dir as well as all user dirs
  RMDir /r "$SMPROGRAMS\EventManager2013"
  SetShellVarContext all
  RMDir /r "$SMPROGRAMS\EventManager2013"

  RMDir /r "$INSTDIR"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager2013"
SectionEnd

Section "Install Java"

  MessageBox MB_YESNO \
    "EventManager requires Java version 6 or later to be installed on your computer. Do you wish to install Java now?" \
     IDYES installjava

  Goto installjavadone

  installjava:

  File ..\thirdparty\jre-6u23-windows-i586-s.exe
  ExecWait "$INSTDIR\jre-6u23-windows-i586-s.exe"

  installjavadone:

  Delete "$INSTDIR\jre-6u23-windows-i586-s.exe"
SectionEnd

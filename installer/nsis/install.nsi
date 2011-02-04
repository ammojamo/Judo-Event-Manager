Name "Event Manager 2011 Update 1"
Caption "Event Manager"
;Icon "YourProgram.ico"
OutFile "EventManagerSetup-2011u1.exe"

LicenseData eula.txt
LicenseForceSelection checkbox

InstallDir $PROGRAMFILES\EventManager

DirText "This will install EventManager 2011 Update 1 on your computer."

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
  createShortCut "$SMPROGRAMS\EventManager\Uninstall.lnk" "$INSTDIR\uninstall.exe"

  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager" \
                   "DisplayName" "EventManager"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager" \
                   "UninstallString" "$\"$INSTDIR\uninstall.exe$\""

SectionEnd

Page license
Page directory
Page instfiles

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

  File ..\thirdparty\jre-6u22-windows-i586-s.exe
  ExecWait "$INSTDIR\jre-6u22-windows-i586-s.exe"

  installjavadone:

  Delete "$INSTDIR\jre-6u22-windows-i586-s.exe"
SectionEnd

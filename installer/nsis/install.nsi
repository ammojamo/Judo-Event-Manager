Name "Event Manager 2014 Update 2"
Caption "Event Manager 2014 Update 2"
;Icon "YourProgram.ico"
OutFile "EventManagerSetup-2014-Update2.exe"

LicenseData eula.txt
LicenseForceSelection checkbox

InstallDir $PROGRAMFILES\EventManager2014

DirText "This will install EventManager 2014 on your computer."

Section ""
  SetOutPath $INSTDIR

;  SetShellVarContext all

  File /r ..\..\resources
  File /r ..\..\dist\*.*
  File ..\..\license.lic
  File icon.ico

  writeUninstaller "$INSTDIR\uninstall.exe"

  CreateDirectory "$SMPROGRAMS\EventManager2014"
  createShortCut "$SMPROGRAMS\EventManager2014\EventManager.lnk" "$INSTDIR\EventManager.jar" "" "$INSTDIR\icon.ico"
  createShortCut "$SMPROGRAMS\EventManager2014\Uninstall.lnk" "$INSTDIR\uninstall.exe"

  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager2014" \
                   "DisplayName" "EventManager"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager2014" \
                   "UninstallString" "$\"$INSTDIR\uninstall.exe$\""

SectionEnd

Page license
Page directory
Page instfiles

UninstallText "This will uninstall EventManager 2014."

Section "Uninstall"
; try user dir as well as all user dirs
  RMDir /r "$SMPROGRAMS\EventManager2014"
  SetShellVarContext all
  RMDir /r "$SMPROGRAMS\EventManager2014"

  RMDir /r "$INSTDIR"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager2014"
SectionEnd

Section "Install Java"

  MessageBox MB_YESNO \
    "EventManager requires Java version 7 or later to be installed on your computer. Do you wish to install Java now?" \
     IDYES installjava

  Goto installjavadone

  installjava:

  File ..\thirdparty\jre-7u51-windows-i586.exe
  ExecWait "$INSTDIR\jre-7u51-windows-i586.exe"

  installjavadone:

  Delete "$INSTDIR\jre-7u51-windows-i586.exe"
SectionEnd

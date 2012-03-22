Name "Event Manager 2012 Update 1 Beta"
Caption "Event Manager 2012 Update 1 Beta"
;Icon "YourProgram.ico"
OutFile "EventManagerSetup-2012-Update1beta.exe"

LicenseData eula.txt
LicenseForceSelection checkbox

InstallDir $PROGRAMFILES\EventManager2012Update1beta

DirText "This will install EventManager 2012 Update1 beta on your computer."

Section ""
  SetOutPath $INSTDIR

;  SetShellVarContext all

  File /r ..\..\resources
  File /r ..\..\dist\*.*
  File ..\..\license.lic
  File icon.ico

  writeUninstaller "$INSTDIR\uninstall.exe"

  CreateDirectory "$SMPROGRAMS\EventManager2012"
  createShortCut "$SMPROGRAMS\EventManager2012\EventManager.lnk" "$INSTDIR\EventManager.jar" "" "$INSTDIR\icon.ico"
  createShortCut "$SMPROGRAMS\EventManager2012\Uninstall.lnk" "$INSTDIR\uninstall.exe"

  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager2012" \
                   "DisplayName" "EventManager"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager2012" \
                   "UninstallString" "$\"$INSTDIR\uninstall.exe$\""

SectionEnd

Page license
Page directory
Page instfiles

UninstallText "This will uninstall EventManager 2012."

Section "Uninstall"
; try user dir as well as all user dirs
  RMDir /r "$SMPROGRAMS\EventManager2012"
  SetShellVarContext all
  RMDir /r "$SMPROGRAMS\EventManager2012"

  RMDir /r "$INSTDIR"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager2012"
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

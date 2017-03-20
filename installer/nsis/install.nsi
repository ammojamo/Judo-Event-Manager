!include x64.nsh

!define YEAR 2017
!define UPDATE 2

Name "Event Manager ${YEAR} Update ${UPDATE}"
Caption "Event Manager ${YEAR} Update ${UPDATE}"
;Icon "YourProgram.ico"

!ifdef NO_JAVA_INSTALLER
OutFile "EventManagerSetup-${YEAR}-Update${UPDATE}.exe"
!else
OutFile "EventManagerSetup-${YEAR}-Update${UPDATE}-Java.exe"
!endif

LicenseData eula.txt
LicenseForceSelection checkbox

InstallDir $PROGRAMFILES\EventManager${YEAR}

DirText "This will install EventManager ${YEAR} on your computer."

Section ""
  SetOutPath $INSTDIR

;  SetShellVarContext all

  File /r ..\..\resources
  File /r ..\..\dist\*.*
  File ..\..\license.lic
  File icon.ico

  writeUninstaller "$INSTDIR\uninstall.exe"

  CreateDirectory "$SMPROGRAMS\EventManager${YEAR}"
  createShortCut "$SMPROGRAMS\EventManager${YEAR}\EventManager.lnk" "$INSTDIR\EventManager.jar" "" "$INSTDIR\icon.ico"
  createShortCut "$SMPROGRAMS\EventManager${YEAR}\Uninstall.lnk" "$INSTDIR\uninstall.exe"

  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager${YEAR}" \
                   "DisplayName" "EventManager"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager${YEAR}" \
                   "UninstallString" "$\"$INSTDIR\uninstall.exe$\""

SectionEnd

Page license
Page directory
Page instfiles

UninstallText "This will uninstall EventManager ${YEAR}."

Section "Uninstall"
; try user dir as well as all user dirs
  RMDir /r "$SMPROGRAMS\EventManager${YEAR}"
  SetShellVarContext all
  RMDir /r "$SMPROGRAMS\EventManager${YEAR}"

  RMDir /r "$INSTDIR"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\EventManager${YEAR}"
SectionEnd


!ifndef NO_JAVA_INSTALLER

Section "Install Java"

  MessageBox MB_YESNO \
    "EventManager requires Java version 7 or later to be installed on your computer. Do you wish to install Java now?" \
     IDYES installjava

  Goto installjavadone

  installjava:

  File ..\thirdparty\jre-8u60-windows-x64.exe
  File ..\thirdparty\jre-8u60-windows-i586.exe

  ${If} ${RunningX64}
    ExecWait "$INSTDIR\jre-8u60-windows-x64.exe"
  ${Else}
    ExecWait "$INSTDIR\jre-8u60-windows-i586.exe"
  ${EndIf}

  installjavadone:

  Delete "$INSTDIR\jre-8u60-windows-x64.exe"
  Delete "$INSTDIR\jre-8u60-windows-i586.exe"
SectionEnd

!endif

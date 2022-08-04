
;--------------------------------
;Include Modern UI

!include "MUI2.nsh"

;--------------------------------
;General

;Name and file
Name "${appName}"
OutFile "${installerExe}"
Unicode True

;Default installation folder
InstallDir "$LOCALAPPDATA\${appName}"

;Get installation folder from registry if available
InstallDirRegKey HKCU "Software\${appName}" ""

; Request application privileges for Windows Vista
RequestExecutionLevel user

;--------------------------------
;Variables

Var StartMenuFolder

;--------------------------------
;Interface Settings

!define MUI_ABORTWARNING
;Start Menu Folder Page Configuration
!define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU"
!define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\${appName}"
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Start Menu Folder"
;--------------------------------
;Pages

!insertmacro MUI_PAGE_WELCOME
<#if licenseFile?hasContent>
!insertmacro MUI_PAGE_LICENSE "${licenseFile}"
</#if>
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuFolder
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

;--------------------------------
;Languages

!insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Installer Sections

Section "Main application files" MainSection

SetOutPath "$INSTDIR"

File /r "${contentDir}/*"

;Store installation folder
WriteRegStr HKCU "Software\${appName}" "" $INSTDIR

;Create uninstaller
WriteUninstaller "$INSTDIR\Uninstall.exe"

!insertmacro MUI_STARTMENU_WRITE_BEGIN Application

;Create shortcuts
CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
CreateShortcut "$SMPROGRAMS\$StartMenuFolder\${appName}.lnk" "$INSTDIR\${exe}.exe"
CreateShortcut "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk" "$INSTDIR\Uninstall.exe"

!insertmacro MUI_STARTMENU_WRITE_END


SectionEnd

;--------------------------------
;Descriptions

;Language strings
LangString DESC_SecDummy ${r"${LANG_ENGLISH}"} "A test section."

;Assign language strings to sections
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${r"${MainSection}"} ${r"$(DESC_SecDummy)"}
!insertmacro MUI_FUNCTION_DESCRIPTION_END

;--------------------------------
;Uninstaller Section

Section "Uninstall"

; Delete installation files
RMDir /r "$INSTDIR"
; Delete Start Menu entries
RMDir /r "$SMPROGRAMS\$StartMenuFolder"

DeleteRegKey /ifempty HKCU "Software\${appName}"

SectionEnd
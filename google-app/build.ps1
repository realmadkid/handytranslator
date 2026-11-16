param([string]$Jdk=$env:JAVA_HOME,[string]$Sdk=$env:ANDROID_HOME)
$ErrorActionPreference='Stop'
if(!$Jdk){
 $javac=Get-Command javac.exe -ErrorAction SilentlyContinue
 if($javac){$Jdk=Split-Path (Split-Path $javac.Source -Parent) -Parent}
}
if(!$Sdk){$Sdk=$env:ANDROID_SDK_ROOT}
if(!$Sdk -and $env:LOCALAPPDATA){$Sdk=Join-Path $env:LOCALAPPDATA 'Android\Sdk'}
if(!$Jdk -or !(Test-Path (Join-Path $Jdk 'bin\javac.exe'))){throw 'Set JAVA_HOME to JDK 17 or pass -Jdk.'}
if(!$Sdk){throw 'Set ANDROID_HOME or pass -Sdk.'}
$root=Split-Path $PSScriptRoot -Parent
$bt=Join-Path $Sdk 'build-tools\35.0.1'
$jar=Join-Path $Sdk 'platforms\android-35\android.jar'
if(!(Test-Path $jar)){throw "Android SDK platform 35 missing: $jar"}
$build=Join-Path $PSScriptRoot 'build'
$release=Join-Path $root 'deliverables'
New-Item -ItemType Directory -Force "$build\classes","$build\dex","$build\test",$release | Out-Null
function Run([string]$exe,[string[]]$arguments){ & $exe @arguments; if($LASTEXITCODE -ne 0){throw "$exe failed: $LASTEXITCODE"} }
$sources=@(Get-ChildItem "$PSScriptRoot\src" -Filter *.java -Recurse | ForEach-Object FullName)
Run "$Jdk\bin\javac.exe" (@('-encoding','UTF-8','--release','8','-classpath',$jar,'-d',"$build\classes")+$sources)
Run "$Jdk\bin\javac.exe" @('-encoding','UTF-8','--release','8','-d',"$build\test","$PSScriptRoot\src\com\handytranslator\google\ButtonGate.java","$PSScriptRoot\src\com\handytranslator\google\MidiParser.java","$PSScriptRoot\test\CoreTest.java")
Run "$Jdk\bin\java.exe" @('-cp',"$build\test",'com.handytranslator.google.CoreTest')
Run "$Jdk\bin\javac.exe" @('-encoding','UTF-8','--release','8','-d',"$build\test","$PSScriptRoot\src\com\handytranslator\google\UsageBudget.java","$PSScriptRoot\test\UsageBudgetTest.java")
Run "$Jdk\bin\java.exe" @('-cp',"$build\test",'com.handytranslator.google.UsageBudgetTest')
Run "$Jdk\bin\jar.exe" @('cf',"$build\classes.jar",'-C',"$build\classes",'.')
Run "$Jdk\bin\java.exe" @('-cp',"$bt\lib\d8.jar",'com.android.tools.r8.D8','--lib',$jar,'--min-api','28','--output',"$build\dex","$build\classes.jar")
Run "$bt\aapt.exe" @('package','-f','-M',"$PSScriptRoot\AndroidManifest.xml",'-I',$jar,'-S',"$PSScriptRoot\res",'-F',"$build\unsigned.apk")
Push-Location "$build\dex"
try{Run "$bt\aapt.exe" @('add',"$build\unsigned.apk",'classes.dex')}finally{Pop-Location}
Run "$bt\zipalign.exe" @('-f','4',"$build\unsigned.apk","$build\aligned.apk")
$key=Join-Path $build 'prototype.keystore'
if(!(Test-Path $key)){Run "$Jdk\bin\keytool.exe" @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','prototype','-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=HandyTranslator Prototype')}
$apk=Join-Path $release 'HandyTranslator-Google-v0.3.apk'
Run "$Jdk\bin\java.exe" @('-jar',"$bt\lib\apksigner.jar",'sign','--ks',$key,'--ks-pass','pass:android','--key-pass','pass:android','--out',$apk,"$build\aligned.apk")
Run "$Jdk\bin\java.exe" @('-jar',"$bt\lib\apksigner.jar",'verify','--verbose',$apk)
Get-FileHash $apk -Algorithm SHA256

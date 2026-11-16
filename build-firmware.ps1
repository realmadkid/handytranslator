param([string]$Cli,[string]$Config)
$ErrorActionPreference='Stop'
if(!$Cli){
 $bundled=Join-Path $PSScriptRoot 'tools\arduino\arduino-cli.exe'
 if(Test-Path $bundled){$Cli=$bundled;if(!$Config){$Config=Join-Path $PSScriptRoot 'tools\arduino.yaml'}}
 else {$command=Get-Command arduino-cli -ErrorAction SilentlyContinue;if(!$command){throw 'Install Arduino CLI and add it to PATH, or pass -Cli.'};$Cli=$command.Source}
}
Push-Location $PSScriptRoot
try {
 $arguments=@('compile','--fqbn','arduino:avr:leonardo','--output-dir','deliverables\firmware','firmware\HandyTranslator')
 if($Config){$arguments+=@('--config-file',$Config)}
 & $Cli @arguments
 if($LASTEXITCODE -ne 0){throw 'Firmware build failed'}
} finally {Pop-Location}

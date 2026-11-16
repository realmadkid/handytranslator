# 빌드와 GitHub 배포

명령은 저장소 루트의 Windows PowerShell에서 실행한다. 프로젝트 코드는 Android SDK와 Java 표준 라이브러리를 사용한다.

## Android 앱

필요한 도구: JDK 17, Android SDK platform 35, build-tools 35.0.1. Android Studio의 SDK Manager로 해당 SDK 구성 요소를 설치할 수 있다.

`JAVA_HOME`은 JDK 디렉터리, `ANDROID_HOME`은 Android SDK 디렉터리로 지정한다. 스크립트는 JDK의 PATH, `ANDROID_SDK_ROOT`, Windows 기본 SDK 위치도 확인한다. 직접 경로를 전달할 수도 있다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File google-app\build.ps1
# 환경변수 대신 설치 위치를 지정하는 예:
# .\google-app\build.ps1 -Jdk 'C:\Java\jdk-17' -Sdk 'C:\Android\Sdk'
```

빌드 중 버튼/MIDI 및 요청 문자 제한 테스트를 실행하고 APK 서명을 검사한다. 결과는 `deliverables/HandyTranslator-Google-v0.3.apk`다.

`google-app/build/prototype.keystore`는 최초 빌드에서 생성하는 개인 시제품용 서명 키다. 기존 앱 업데이트와 Google API Android 앱 제한 지문을 유지하려면 이 파일을 로컬에 보존한다. Git 또는 공개 ZIP에는 넣지 않는다. 새 컴퓨터에서 다른 키로 빌드하면 APK 지문도 달라지므로 [설정 안내](GOOGLE-SETUP.md)의 인증서 지문을 그대로 적용하지 않는다. 현재 빌드는 개인 시제품용이며 일반 배포용 서명 관리는 별도다.

## 보드 펌웨어

Arduino CLI를 설치하여 PATH에 등록한다. 검증한 의존성은 AVR core 1.8.8, MIDIUSB 1.0.5다.

```powershell
arduino-cli core update-index
arduino-cli core install arduino:avr@1.8.8
arduino-cli lib install MIDIUSB@1.0.5
powershell -NoProfile -ExecutionPolicy Bypass -File build-firmware.ps1
```

로컬 `tools/arduino/arduino-cli.exe`와 `tools/arduino.yaml`이 있으면 기존 개발 환경을 사용한다. 다른 CLI나 설정은 `-Cli`, `-Config`로 지정할 수 있다. 출력은 `deliverables/firmware/`다. 폰트나 화면 변경만으로 펌웨어를 다시 올릴 필요는 없다.

## 공개용 소스 패키지

Python 3.9+ 표준 라이브러리만 사용한다.

```powershell
python scripts/package_release.py
```

명시된 소스·문서·사진·빌드 스크립트만 포함한다. `tools/`, `build/`, 인증 정보, 서명 키, 기존 배포 ZIP은 포함하지 않는다. 현재 APK가 있으면 APK 해시도 함께 갱신한다. 소스 ZIP에는 프로토타입 사진이 포함된다.

## GitHub 업로드 대상

저장소에는 아래 항목을 올린다.

```text
.gitattributes
.gitignore
README.md
LICENSE
build-firmware.ps1
google-app/       (build 제외)
firmware/
docs/
scripts/
```

`deliverables/`는 Git 추적 대상에서 제외한다. 릴리스할 때 APK, 펌웨어 `HandyTranslator.ino.hex`, 소스 ZIP, SHA256 파일을 GitHub Releases 첨부 파일로 올린다. 아직 릴리스 URL은 정하지 않았다.

Git으로 추가할 때는 `.gitignore`를 적용한 뒤 `git status --short`로 목록을 확인한다. GitHub 웹에서 업로드한다면 프로젝트 폴더 전체를 끌어 넣지 말고 위 항목 또는 공개용 소스 ZIP의 내용만 선택한다. 저장소 주소는 https://github.com/realmadkid/handytranslator 이다.

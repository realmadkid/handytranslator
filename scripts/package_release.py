"""Package only public project inputs; no toolchains, build caches or signing keys."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
version = ET.parse(ROOT / "google-app/AndroidManifest.xml").getroot().get(ANDROID_NS + "versionName").split("-")[0]
prefix = f"HandyTranslator-Google-v{version}"
files = [ROOT / name for name in (".gitignore", ".gitattributes", "README.md", "LICENSE", "build-firmware.ps1")]
extensions = {
    "google-app": {".java", ".xml", ".ps1"},
    "firmware": {".ino", ".h", ".cpp"},
    "docs": {".md", ".jpg", ".jpeg", ".png", ".svg"},
    "scripts": {".py", ".ps1"},
}
for folder, allowed in extensions.items():
    for path in (ROOT / folder).rglob("*"):
        relative = path.relative_to(ROOT)
        if path.is_file() and path.suffix.lower() in allowed and not {"build", "__pycache__"}.intersection(relative.parts):
            if path.is_symlink() or not path.resolve().is_relative_to(ROOT):
                raise SystemExit(f"Unexpected path: {relative}")
            files.append(path)

# Do not print credential values if a public input accidentally contains one.
secret_patterns = [rb"AIza[0-9A-Za-z_-]{35}", rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"]
for path in files:
    if path.suffix.lower() not in {".jpg", ".jpeg", ".png"}:
        if any(re.search(pattern, path.read_bytes()) for pattern in secret_patterns):
            raise SystemExit(f"Possible credential in {path.relative_to(ROOT)}; package not created")

output = ROOT / "deliverables"
output.mkdir(exist_ok=True)
archive = output / f"{prefix}-source.zip"
with ZipFile(archive, "w", ZIP_DEFLATED) as zipped:
    for path in sorted(set(files)):
        zipped.write(path, path.relative_to(ROOT).as_posix())
with ZipFile(archive) as zipped:
    if zipped.testzip() is not None:
        raise SystemExit("Archive integrity check failed")

artifacts = [output / f"{prefix}.apk", archive, output / "firmware/HandyTranslator.ino.hex"]
checksums = [f"{hashlib.sha256(path.read_bytes()).hexdigest().upper()}  {path.relative_to(output).as_posix()}"
             for path in artifacts if path.is_file()]
(output / f"SHA256SUMS-Google-v{version}.txt").write_text("\n".join(checksums) + "\n", encoding="utf-8")
print(f"Packaged {len(set(files))} public files: {archive.name}")

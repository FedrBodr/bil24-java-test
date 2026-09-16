#!/usr/bin/env python3
import hashlib
from io import BytesIO
from pathlib import Path
import urllib.request
from zipfile import ZipFile


ARCHIVE_URL = "https://bil24.pro/test/test_src.zip"
ARCHIVE_SHA256 = "53be3446279e6312167e100cd86c66525801fcde78d3fac918f3ec1ed591a1b2"
SOURCE_NAMES = (
    "src/test/ParserSeat.java",
    "src/test/ResultParser.java",
)


def verify_archive(data: bytes, expected_sha256: str) -> None:
    if hashlib.sha256(data).hexdigest() != expected_sha256:
        raise ValueError("BIL24 archive SHA-256 mismatch")


def relocate_source(data: bytes) -> bytes:
    old = b"package test;"
    if data.count(old) != 1:
        raise ValueError("Expected exactly one package declaration")
    return data.replace(old, b"package reference;", 1)


def main() -> None:
    project_root = Path(__file__).resolve().parents[1]
    cache_dir = project_root / ".cache" / "bil24"
    archive_path = cache_dir / "test_src.zip"

    if archive_path.exists():
        archive_data = archive_path.read_bytes()
    else:
        with urllib.request.urlopen(ARCHIVE_URL, timeout=30) as response:
            archive_data = response.read()

    verify_archive(archive_data, ARCHIVE_SHA256)
    cache_dir.mkdir(parents=True, exist_ok=True)
    archive_path.write_bytes(archive_data)

    reference_dir = cache_dir / "reference-src" / "reference"
    reference_dir.mkdir(parents=True, exist_ok=True)
    written_paths = []
    with ZipFile(BytesIO(archive_data)) as archive:
        for source_name in SOURCE_NAMES:
            destination = reference_dir / Path(source_name).name
            destination.write_bytes(relocate_source(archive.read(source_name)))
            written_paths.append(destination)

    paths = ", ".join(
        str(path.relative_to(project_root)) for path in written_paths
    )
    print(f"Verified BIL24 reference {ARCHIVE_SHA256}; wrote {paths}")


if __name__ == "__main__":
    main()

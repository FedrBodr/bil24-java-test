import hashlib
import io
import unittest
import zipfile

from fetch_reference import relocate_source, verify_archive


class FetchReferenceTest(unittest.TestCase):
    def test_rejects_archive_with_wrong_sha256(self):
        with self.assertRaisesRegex(ValueError, "SHA-256 mismatch"):
            verify_archive(b"archive", "0" * 64)

    def test_accepts_archive_with_expected_sha256(self):
        data = b"archive"
        verify_archive(data, hashlib.sha256(data).hexdigest())

    def test_package_relocation_preserves_each_source(self):
        sources = {
            "src/test/ParserSeat.java": (
                b"package test;\r\n\r\npublic class ParserSeat {}\r\n"
            ),
            "src/test/ResultParser.java": (
                b"package test;\n\npublic class ResultParser {}\n"
            ),
        }
        archive_buffer = io.BytesIO()
        with zipfile.ZipFile(archive_buffer, "w") as archive:
            for name, source in sources.items():
                archive.writestr(name, source)

        with zipfile.ZipFile(io.BytesIO(archive_buffer.getvalue())) as archive:
            for name, source in sources.items():
                with self.subTest(name=name):
                    relocated = relocate_source(archive.read(name))
                    restored = relocated.replace(
                        b"package reference;", b"package test;", 1
                    )
                    self.assertEqual(source, restored)

    def test_rejects_source_without_package_declaration(self):
        with self.assertRaisesRegex(ValueError, "exactly one package declaration"):
            relocate_source(b"public class Example {}\n")

    def test_rejects_source_with_repeated_package_declaration(self):
        source = b"package test;\npackage test;\npublic class Example {}\n"
        with self.assertRaisesRegex(ValueError, "exactly one package declaration"):
            relocate_source(source)


if __name__ == "__main__":
    unittest.main()

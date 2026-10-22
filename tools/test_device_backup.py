"""Host regression: a failed backup must never trigger device-data restoration."""
import pathlib
import runpy
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parent

class BackupFailureTest(unittest.TestCase):
    def assert_backup_failure_is_read_only(self, script):
        commands = []
        reads = 0
        def fake_run(command, **kwargs):
            nonlocal reads
            args = command[3:]  # adb -s SERIAL
            commands.append(args)
            output = b""; error = b""; code = 0
            if args[:3] == ["shell", "pm", "path"]:
                output = b"package:/test/base.apk\n"
            elif "find" in args:
                output = b"./databases/a.db\n./databases/b.db\n"
            elif "run-as" in args and "cat" in args:
                reads += 1
                if reads == 1: output = b"original-data"
                else: code = 1; error = b"Permission denied"
            elif "settings" in args: output = b"0"
            return subprocess.CompletedProcess(command, code, output, error)
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(sys, "argv", [script, "--serial", "test", "--output", directory]), patch("subprocess.run", side_effect=fake_run):
                with self.assertRaises((RuntimeError, SystemExit)):
                    runpy.run_path(str(ROOT / script), run_name="__main__")
        self.assertEqual(2, reads)
        for command in commands:
            self.assertFalse(any(word in command for word in ("rm", "tee", "instrument", "put", "delete")), command)

    def test_tv1_partial_backup_never_overwrites_or_removes_data(self):
        self.assert_backup_failure_is_read_only("test-tv1-device.py")

    def test_tv3_partial_backup_never_overwrites_or_removes_data(self):
        self.assert_backup_failure_is_read_only("test-tv3-device.py")

if __name__ == "__main__": unittest.main()

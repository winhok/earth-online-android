#!/usr/bin/env python3
"""Regression for duplicate recovery-choice and contract-history titles."""
import importlib.util
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('release_smoke', Path(__file__).with_name('release-smoke.py'))
ui = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ui)


class RecoveryChoiceSelectorTest(unittest.TestCase):
    def test_only_the_checkable_row_matches_not_history_or_title(self):
        root = ET.fromstring('''<hierarchy>
          <node checkable="true" clickable="true" checked="false" bounds="[53,872][1027,1019]">
            <node text="ExpiredPromise2" bounds="[137,914][955,977]" />
          </node>
          <node checkable="false" clickable="false" bounds="[53,1710][1027,1794]">
            <node text="ExpiredPromise2" bounds="[95,1752][430,1794]" />
          </node>
        </hierarchy>''')
        found = [node for node in root.iter('node') if ui.is_checkable_label('ExpiredPromise2')(node)]
        self.assertEqual(len(found), 1)
        self.assertEqual(found[0].get('bounds'), '[53,872][1027,1019]')

    def test_non_clickable_or_different_choice_does_not_match(self):
        for attributes in (
            {'checkable': 'true', 'clickable': 'false'},
            {'checkable': 'false', 'clickable': 'true'},
            {'checkable': 'true', 'clickable': 'true'},
        ):
            row = ET.Element('node', attributes)
            ET.SubElement(row, 'node', text='OtherTask')
            self.assertFalse(ui.is_checkable_label('ExpiredPromise2')(row))


class DocumentSaveSelectorTest(unittest.TestCase):
    def test_actual_documents_ui_button_matches_in_both_languages(self):
        for text in ('SAVE', '保存'):
            button = ET.Element('node', {
                'text': text, 'package': 'com.android.documentsui',
                'class': 'android.widget.Button', 'clickable': 'true', 'enabled': 'true',
            })
            self.assertTrue(ui.is_document_save_button(button))

    def test_stale_app_save_label_and_disabled_button_do_not_match(self):
        stale = ET.Element('node', {
            'text': '保存', 'package': 'xyz.winhok.earthonline',
            'class': 'android.widget.TextView', 'clickable': 'false', 'enabled': 'true',
        })
        self.assertFalse(ui.is_document_save_button(stale))
        stale.set('package', 'com.android.documentsui')
        stale.set('class', 'android.widget.Button')
        stale.set('clickable', 'true')
        stale.set('enabled', 'false')
        self.assertFalse(ui.is_document_save_button(stale))


if __name__ == '__main__':
    unittest.main()

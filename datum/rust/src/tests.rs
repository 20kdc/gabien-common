/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use alloc::vec::Vec;
use alloc::string::{String, ToString};

use crate::{datum_byte_to_value_pipeline, datum_char_to_token_pipeline, datum_char_to_value_pipeline, DatumPipe, DatumToken, DatumTokenType, DatumWriter};

fn do_roundtrip_test(input: &str, output: &str) {
    let mut dectok1 = datum_char_to_token_pipeline();
    let mut ignoredout = Vec::new();
    dectok1.feed_iter_to_vec(&mut ignoredout, input.chars(), true);
    assert!(!dectok1.has_error());
    // ---
    let mut dtparse = datum_char_to_value_pipeline();
    let mut out = Vec::new();
    dtparse.feed_iter_to_vec(&mut out, input.chars(), true);
    // so, fun fact, in all the refactors, a bug snuck in where starting any list would enable the parse error flag
    assert!(!dtparse.has_error());
    let mut out_str = String::new();
    let mut writer = DatumWriter::default();
    for v in out {
        v.write_to(&mut out_str, &mut writer).unwrap();
    }
    assert_eq!(out_str, output);
    // --- same again but with bytes
    let mut dtparse = datum_byte_to_value_pipeline();
    let mut out = Vec::new();
    dtparse.feed_iter_to_vec(&mut out, input.bytes(), true);
    assert!(!dtparse.has_error());
    let mut out_str = String::new();
    let mut writer = DatumWriter::default();
    for v in out {
        v.write_to(&mut out_str, &mut writer).unwrap();
    }
    assert_eq!(out_str, output);
}

fn tokenizer_should_error_eof(input: &str) {
    let mut dectok1 = datum_char_to_token_pipeline();
    let mut ignoredout = Vec::new();
    dectok1.feed_iter_to_vec(&mut ignoredout, input.chars(), false);
    assert!(!dectok1.has_error());
    dectok1.eof_to_vec(&mut ignoredout);
    assert!(dectok1.has_error());
}

#[test]
fn test_token_write() {
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::String, "Test\\\r\n\t").to_string(), "\"Test\\\\\\r\\n\\t\"");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::ID, "").to_string(), "#{}#");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::ID, "test").to_string(), "test");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::SpecialID, "test").to_string(), "#test");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::Numeric, "-").to_string(), "#i-");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::Numeric, "-1.0").to_string(), "-1.0");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::Numeric, "1").to_string(), "1");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::Numeric, "AA").to_string(), "#iAA");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::Numeric, "\\A").to_string(), "#i\\\\A");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::Numeric, "").to_string(), "#i");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::Quote, "").to_string(), "'");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::ListStart, "").to_string(), "(");
    assert_eq!(&DatumToken::new_not_into(DatumTokenType::ListEnd, "").to_string(), ")");
}

#[test]
fn roundtrip_tests() {
    do_roundtrip_test("", "");
    do_roundtrip_test("hello", "hello");
    do_roundtrip_test("(hello)", "(hello)");
    do_roundtrip_test("1.23", "1.23");
    do_roundtrip_test("#i+nan.0", "#i+nan.0");
    do_roundtrip_test("#i+inf.0", "#i+inf.0");
    do_roundtrip_test("#i-inf.0", "#i-inf.0");
    do_roundtrip_test("10", "10");
    do_roundtrip_test("-10", "-10");
    do_roundtrip_test("-", "-");
    do_roundtrip_test("#t", "#t");
    do_roundtrip_test("#f", "#f");
    do_roundtrip_test("#nil", "#nil");
    do_roundtrip_test("#{}#", "#{}#");
    do_roundtrip_test("'hello", "(quote hello)");
    do_roundtrip_test("\"mi moku telo tan luka sina\"", "\"mi moku telo tan luka sina\"");
    do_roundtrip_test("escape\\ me", "escape\\ me");
    do_roundtrip_test("; line comment\n", "");

    tokenizer_should_error_eof("\"a");

    // writer silly tests
    let mut writer_fmt = String::new();
    let mut writer_fmt_test = DatumWriter::default();
    writer_fmt_test.write_newline(&mut writer_fmt).unwrap();
    writer_fmt_test.indent = 1;
    writer_fmt_test.write_comment(&mut writer_fmt, "lof\nlif\nidk?").unwrap();
    assert_eq!(writer_fmt, "\n\t; lof\n\t; lif\n\t; idk?\n");
}

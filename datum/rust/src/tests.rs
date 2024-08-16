/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use alloc::vec::Vec;
use alloc::string::String;

use crate::{DatumComposePipe, DatumDecoder, DatumParser, DatumPipe, DatumStringTokenizer, DatumWriter, DATUM_DECODER_TOKENIZER_MAX_SIZE, DATUM_PARSER_MAX_SIZE};

fn do_roundtrip_test(input: &str, output: &str) {
    let mut dectok1: DatumComposePipe<_, _, {DATUM_DECODER_TOKENIZER_MAX_SIZE}> = DatumComposePipe(DatumDecoder::default(), DatumStringTokenizer::default());
    let mut ignoredout = Vec::new();
    dectok1.feed_iter_to_vec(&mut ignoredout, input.chars(), true);
    assert!(!dectok1.has_error());
    // ---
    let dectok: DatumComposePipe<_, _, {DATUM_DECODER_TOKENIZER_MAX_SIZE}> = DatumComposePipe(DatumDecoder::default(), DatumStringTokenizer::default());
    let mut dtparse: DatumComposePipe<_, _, {DATUM_DECODER_TOKENIZER_MAX_SIZE * DATUM_PARSER_MAX_SIZE * 2}> = DatumComposePipe(dectok, DatumParser::default());
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
    do_roundtrip_test("#t", "#t");
    do_roundtrip_test("#f", "#f");
    do_roundtrip_test("#nil", "#nil");
    do_roundtrip_test("'hello", "(quote hello)");
    do_roundtrip_test("\"mi moku telo tan luka sina\"", "\"mi moku telo tan luka sina\"");
    // writer silly tests
    let mut writer_fmt = String::new();
    let mut writer_fmt_test = DatumWriter::default();
    writer_fmt_test.write_newline(&mut writer_fmt).unwrap();
    writer_fmt_test.indent = 1;
    writer_fmt_test.write_comment(&mut writer_fmt, "lof\nlif\nidk?").unwrap();
    assert_eq!(writer_fmt, "\n\t; lof\n\t; lif\n\t; idk?\n");
}

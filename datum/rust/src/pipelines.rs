/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use alloc::string::String;

use crate::{DatumComposePipe, DatumDecoder, DatumParser, DatumPipe, DatumStringTokenizer, DatumToken, DatumUTF8Decoder, DatumValue, DATUM_DECODER_MAX_SIZE, DATUM_PARSER_MAX_SIZE, DATUM_TOKENIZER_MAX_SIZE, DATUM_UTF8_DECODER_MAX_SIZE};

// -- token outputting --

/// Utility for composed utf8/decoder/tokenizer size.
pub const DATUM_BYTE_TO_TOKEN_MAX_SIZE: usize = DATUM_UTF8_DECODER_MAX_SIZE * DATUM_DECODER_MAX_SIZE * DATUM_TOKENIZER_MAX_SIZE * 2 * 2;

/// Utility for composed decoder/tokenizer size.
pub const DATUM_CHAR_TO_TOKEN_MAX_SIZE: usize = DATUM_DECODER_MAX_SIZE * DATUM_TOKENIZER_MAX_SIZE * 2;

/// Byte to token parsing pipeline.
pub fn datum_byte_to_token_pipeline() -> impl DatumPipe<Input = u8, Output = DatumToken<String>> {
    let utf2chr: DatumComposePipe<_, _, {DATUM_UTF8_DECODER_MAX_SIZE * DATUM_DECODER_MAX_SIZE * 2}> = DatumComposePipe(DatumUTF8Decoder::default(), DatumDecoder::default());
    let dtparse: DatumComposePipe<_, _, {DATUM_BYTE_TO_TOKEN_MAX_SIZE}> = DatumComposePipe(utf2chr, DatumStringTokenizer::default());
    dtparse
}

/// Character to token parsing pipeline.
pub fn datum_char_to_token_pipeline() -> impl DatumPipe<Input = char, Output = DatumToken<String>> {
    let dectok: DatumComposePipe<_, _, {DATUM_CHAR_TO_TOKEN_MAX_SIZE}> = DatumComposePipe(DatumDecoder::default(), DatumStringTokenizer::default());
    dectok
}

// -- value outputting --

/// Utility for composed utf8/decoder/tokenizer/parser size.
pub const DATUM_BYTE_TO_VALUE_MAX_SIZE: usize = DATUM_BYTE_TO_TOKEN_MAX_SIZE * DATUM_PARSER_MAX_SIZE * 2;

/// Utility for composed decoder/tokenizer/parser size.
pub const DATUM_CHAR_TO_VALUE_MAX_SIZE: usize = DATUM_CHAR_TO_TOKEN_MAX_SIZE * DATUM_PARSER_MAX_SIZE * 2;

/// Byte to value parsing pipeline.
pub fn datum_byte_to_value_pipeline() -> impl DatumPipe<Input = u8, Output = DatumValue> {
    let tokenizer = datum_byte_to_token_pipeline();
    let dtparse: DatumComposePipe<_, _, {DATUM_BYTE_TO_TOKEN_MAX_SIZE * DATUM_PARSER_MAX_SIZE * 2}> = DatumComposePipe(tokenizer, DatumParser::default());
    dtparse
}

/// Char to value parsing pipeline.
pub fn datum_char_to_value_pipeline() -> impl DatumPipe<Input = char, Output = DatumValue> {
    let tokenizer = datum_char_to_token_pipeline();
    let dtparse: DatumComposePipe<_, _, {DATUM_CHAR_TO_TOKEN_MAX_SIZE * DATUM_PARSER_MAX_SIZE * 2}> = DatumComposePipe(tokenizer, DatumParser::default());
    dtparse
}

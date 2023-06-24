// gabien-common - Cross-platform game and UI framework
// Written starting in 2016 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.

// This is used to create the reproducible gabien-natives file.
// The philosophy of this program is something along the lines of "Compression? What compression?"
// The contents of this zip are extracted and recompressed anyway before they hit end users.

const std = @import("std");

// for recording central directory record
const CDRecord = struct {
	name: []const u8,
	offset: u32,
	crc32: u32,
	size: u32,
	next: ?*CDRecord
};

pub fn main() anyerror!void {
	var gpa1 = std.heap.GeneralPurposeAllocator(.{}){};
	const gpa2 = gpa1.allocator();
	var args = try std.process.argsWithAllocator(gpa2);
	_ = args.next();
	var bitWriter = std.io.bitWriter(std.builtin.Endian.Little, std.io.getStdOut().writer());
	var currentOffset: u32 = 0;
	var first: ?*CDRecord = null;
	var nxt: *?*CDRecord = &first;
	const u16_0 = @as(u16, 0);
	while (true) {
		const argQ = args.next();
		if (argQ == null)
			break;
		// get details
		const arg = argQ.?;
		const file = try std.fs.cwd().readFileAlloc(gpa2, arg, 0x7FFFFFFF);
		defer gpa2.free(file);
		// create central directory record
		var cdr = CDRecord{
			.crc32 = @intCast(u32, std.hash.Crc32.hash(file)),
			.name = try gpa2.dupe(u8, arg),
			.offset = currentOffset,
			.size = @intCast(u32, file.len),
			.next = null
		};
		// put onto list
		const cdrSaved = try gpa2.create(CDRecord);
		cdrSaved.* = cdr;
		nxt.* = cdrSaved;
		nxt = &cdrSaved.next;
		// continue
		try bitWriter.writeBits(@as(u32, 0x04034b50), 32);
		try bitWriter.writeBits(u16_0, 16); // v
		try bitWriter.writeBits(u16_0, 16); // fl
		try bitWriter.writeBits(u16_0, 16); // cm
		try bitWriter.writeBits(u16_0, 16); // t
		try bitWriter.writeBits(u16_0, 16); // d
		try bitWriter.writeBits(cdr.crc32, 32); // crc32
		try bitWriter.writeBits(cdr.size, 32); // compressed
		try bitWriter.writeBits(cdr.size, 32); // uncompressed
		try bitWriter.writeBits(@intCast(u16, arg.len), 16); // fnl
		try bitWriter.writeBits(u16_0, 16); // efl
		try bitWriter.flushBits();
		currentOffset += 30;
		_ = try bitWriter.write(arg);
		currentOffset += @intCast(u32, arg.len);
		_ = try bitWriter.write(file);
		currentOffset += @intCast(u32, file.len);
	}
	// generate central directory
	var cdStart: u32 = currentOffset;
	var recordCount: u16 = 0;
	while (first != null) {
		const cdr = first.?.*;
		// write central directory record
		try bitWriter.writeBits(@as(u32, 0x02014b50), 32);
		try bitWriter.writeBits(u16_0, 16);
		try bitWriter.writeBits(u16_0, 16);
		try bitWriter.writeBits(u16_0, 16);
		try bitWriter.writeBits(u16_0, 16);
		try bitWriter.writeBits(u16_0, 16);
		try bitWriter.writeBits(u16_0, 16);
		try bitWriter.writeBits(cdr.crc32, 32); // crc32
		try bitWriter.writeBits(cdr.size, 32); // compressed
		try bitWriter.writeBits(cdr.size, 32); // uncompressed
		try bitWriter.writeBits(@intCast(u16, cdr.name.len), 16); // fnl
		try bitWriter.writeBits(u16_0, 16); // efl
		try bitWriter.writeBits(u16_0, 16); // fcl
		try bitWriter.writeBits(u16_0, 16); // dnf
		try bitWriter.writeBits(u16_0, 16); // ifa
		try bitWriter.writeBits(@as(u32, 0), 32); // efa
		try bitWriter.writeBits(@as(u32, cdr.offset), 32); // offset
		try bitWriter.flushBits();
		currentOffset += 46;
		_ = try bitWriter.write(cdr.name);
		currentOffset += @intCast(u32, cdr.name.len);
		// done, next
		recordCount += 1;
		first = cdr.next;
	}
	var cdSize: u32 = currentOffset - cdStart;
	// generate EOCD
	try bitWriter.writeBits(@as(u32, 0x06054b50), 32);
	try bitWriter.writeBits(u16_0, 16);
	try bitWriter.writeBits(u16_0, 16);
	try bitWriter.writeBits(recordCount, 16);
	try bitWriter.writeBits(recordCount, 16);
	try bitWriter.writeBits(cdSize, 32);
	try bitWriter.writeBits(cdStart, 32);
	try bitWriter.writeBits(u16_0, 16); // cl
	try bitWriter.flushBits();
}

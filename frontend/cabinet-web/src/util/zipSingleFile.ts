function crc32(bytes: Uint8Array): number {
    const table = new Uint32Array(256);

    for (let index = 0; index < 256; index++) {
        let value = index;
        for (let bit = 0; bit < 8; bit++) {
            value = (value & 1) ? (0xedb88320 ^ (value >>> 1)) : (value >>> 1);
        }
        table[index] = value >>> 0;
    }

    let crc = 0xffffffff;

    for (let index = 0; index < bytes.length; index++) {
        crc = table[(crc ^ bytes[index]) & 0xff] ^ (crc >>> 8);
    }

    return (crc ^ 0xffffffff) >>> 0;
}

function dosDateTime(date: Date) {
    const year = Math.max(1980, Math.min(2107, date.getFullYear()));
    const time = (date.getHours() << 11) | (date.getMinutes() << 5) | (date.getSeconds() >> 1);
    const day = ((year - 1980) << 9) | ((date.getMonth() + 1) << 5) | date.getDate();

    return { time, day };
}

function writeUint16(target: Uint8Array, offset: number, value: number) {
    new DataView(target.buffer, target.byteOffset, target.byteLength).setUint16(offset, value, true);
}

function writeUint32(target: Uint8Array, offset: number, value: number) {
    new DataView(target.buffer, target.byteOffset, target.byteLength).setUint32(offset, value, true);
}

export function zipSingleFile(fileName: string, fileBytes: Uint8Array, modifiedAt = new Date()): Blob {
    const nameBytes = new TextEncoder().encode(fileName);
    const crc = crc32(fileBytes);
    const { time, day } = dosDateTime(modifiedAt);

    const localHeader = new Uint8Array(30 + nameBytes.length);
    writeUint32(localHeader, 0, 0x04034b50);
    writeUint16(localHeader, 4, 20);
    writeUint16(localHeader, 6, 0x0800);
    writeUint16(localHeader, 8, 0);
    writeUint16(localHeader, 10, time);
    writeUint16(localHeader, 12, day);
    writeUint32(localHeader, 14, crc);
    writeUint32(localHeader, 18, fileBytes.length);
    writeUint32(localHeader, 22, fileBytes.length);
    writeUint16(localHeader, 26, nameBytes.length);
    writeUint16(localHeader, 28, 0);
    localHeader.set(nameBytes, 30);

    const centralHeader = new Uint8Array(46 + nameBytes.length);
    writeUint32(centralHeader, 0, 0x02014b50);
    writeUint16(centralHeader, 4, 20);
    writeUint16(centralHeader, 6, 20);
    writeUint16(centralHeader, 8, 0x0800);
    writeUint16(centralHeader, 10, 0);
    writeUint16(centralHeader, 12, time);
    writeUint16(centralHeader, 14, day);
    writeUint32(centralHeader, 16, crc);
    writeUint32(centralHeader, 20, fileBytes.length);
    writeUint32(centralHeader, 24, fileBytes.length);
    writeUint16(centralHeader, 28, nameBytes.length);
    writeUint16(centralHeader, 30, 0);
    writeUint16(centralHeader, 32, 0);
    writeUint16(centralHeader, 34, 0);
    writeUint16(centralHeader, 36, 0);
    writeUint32(centralHeader, 38, 0);
    writeUint32(centralHeader, 42, 0);
    centralHeader.set(nameBytes, 46);

    const endOfCentralDirectory = new Uint8Array(22);
    const centralDirectoryOffset = localHeader.length + fileBytes.length;
    writeUint32(endOfCentralDirectory, 0, 0x06054b50);
    writeUint16(endOfCentralDirectory, 4, 0);
    writeUint16(endOfCentralDirectory, 6, 0);
    writeUint16(endOfCentralDirectory, 8, 1);
    writeUint16(endOfCentralDirectory, 10, 1);
    writeUint32(endOfCentralDirectory, 12, centralHeader.length);
    writeUint32(endOfCentralDirectory, 16, centralDirectoryOffset);
    writeUint16(endOfCentralDirectory, 20, 0);

    return new Blob([localHeader, fileBytes, centralHeader, endOfCentralDirectory], {
        type: "application/zip",
    });
}
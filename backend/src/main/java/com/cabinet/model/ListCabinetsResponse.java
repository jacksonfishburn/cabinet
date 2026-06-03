package com.cabinet.model;

import java.util.List;

public record ListCabinetsResponse(
        List<CabinetInfo> cabinets
) {}
package org.example;

import java.util.List;

public record Row(List<Long> hashedRow, int hash) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Row byteRow = (Row) o;
        return hash == byteRow.hash;
    }

    @Override
    public int hashCode() {
        return hash;
    }
}

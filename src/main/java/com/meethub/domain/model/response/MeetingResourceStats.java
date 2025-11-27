// MeetingResourceStats.java
package com.meethub.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResourceStats {
    private Long totalResources;
    private Long documentCount;
    private Long presentationCount;
    private Long imageCount;
    private Long videoCount;
    private Long audioCount;
    private Long otherCount;
    private Long totalSize; // całkowity rozmiar w bajtach
    private String totalSizeFormatted;

    // Metoda pomocnicza do formatowania rozmiaru
    public String getTotalSizeFormatted() {
        if (totalSize == null) return "0 B";

        if (totalSize < 1024) {
            return totalSize + " B";
        } else if (totalSize < 1024 * 1024) {
            return String.format("%.1f KB", totalSize / 1024.0);
        } else if (totalSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", totalSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", totalSize / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
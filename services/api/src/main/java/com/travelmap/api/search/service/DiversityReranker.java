package com.travelmap.api.search.service;

import com.travelmap.api.poi.service.PoiNameNormalizer;
import com.travelmap.api.search.dto.SearchResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Đa dạng hoá kết quả trang đầu (phần 2.3).
 *
 * <p>Sau khi danh sách đã được xếp hạng và sort theo điểm, reranker làm hai việc:
 * <ul>
 *   <li><b>Gộp chi nhánh trùng tên</b> — nhiều quán cùng một chuỗi (ví dụ
 *       "Highlands Coffee" ở nhiều địa chỉ) chỉ giữ lại quán đầu tiên ở vùng đầu,
 *       các chi nhánh còn lại bị đẩy xuống cuối.</li>
 *   <li><b>Giới hạn số quán cùng loại</b> — mỗi {@code category} chỉ cho tối đa
 *       {@code categoryCap} quán ở vùng đầu, phần dư đẩy xuống cuối.</li>
 * </ul>
 *
 * <p><b>Bất biến quan trọng:</b> reranker KHÔNG làm mất kết quả nào — nó chỉ đổi
 * thứ tự. Các phần tử bị đẩy xuống ("overflow") vẫn nằm ở cuối danh sách trả về,
 * giữ nguyên thứ tự xếp hạng tương đối giữa chúng. Nhờ vậy tổng số kết quả và việc
 * phân trang phía sau không bị ảnh hưởng.
 */
@Service
public class DiversityReranker {

    /** Số quán tối đa cho mỗi loại ở vùng đầu, dùng khi gọi không truyền cap riêng. */
    public static final int DEFAULT_CATEGORY_CAP = 3;

    private final PoiNameNormalizer nameNormalizer;

    public DiversityReranker(PoiNameNormalizer nameNormalizer) {
        this.nameNormalizer = nameNormalizer;
    }

    /**
     * Sắp xếp lại danh sách đã xếp hạng để đa dạng hoá vùng đầu.
     *
     * @param ranked      danh sách đã sort theo điểm (giữ nguyên, không bị sửa)
     * @param categoryCap số quán tối đa cho mỗi loại ở vùng đầu ({@code <= 0} nghĩa là không giới hạn)
     * @param dedupChain  có gộp các chi nhánh trùng tên hay không
     * @return danh sách mới cùng kích thước: phần "được chọn" ở đầu, phần "dư" ở cuối
     */
    public List<SearchResult> rerank(List<SearchResult> ranked, int categoryCap, boolean dedupChain) {
        List<SearchResult> primary = new ArrayList<>();
        List<SearchResult> overflow = new ArrayList<>();
        Set<String> seenChain = new HashSet<>();
        Map<String, Integer> perCategory = new HashMap<>();

        for (SearchResult result : ranked) {
            if (dedupChain) {
                String key = nameNormalizer.normalize(result.name());
                if (!seenChain.add(key)) {
                    // Đã có một quán cùng tên chuẩn hoá ở vùng đầu → đây là chi nhánh trùng.
                    overflow.add(result);
                    continue;
                }
            }

            if (categoryCap > 0) {
                int count = perCategory.merge(result.category(), 1, Integer::sum);
                if (count > categoryCap) {
                    overflow.add(result);
                    continue;
                }
            }

            primary.add(result);
        }

        // Không mất kết quả: phần dư nối vào cuối, giữ nguyên thứ tự xếp hạng của chúng.
        primary.addAll(overflow);
        return primary;
    }
}

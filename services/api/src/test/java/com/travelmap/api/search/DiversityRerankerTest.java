package com.travelmap.api.search;

import com.travelmap.api.poi.service.PoiNameNormalizer;
import com.travelmap.api.search.dto.ScoreDetail;
import com.travelmap.api.search.dto.SearchResult;
import com.travelmap.api.search.service.DiversityReranker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiversityRerankerTest {

    private final DiversityReranker reranker = new DiversityReranker(new PoiNameNormalizer());

    @Test
    void gop_chi_nhanh_trung_ten() {
        // Ba quán cùng tên "Highlands Coffee" (3 chi nhánh) + 1 quán khác tên.
        var branch1 = result("Highlands Coffee", "cafe");
        var other = result("Cộng Cà Phê", "cafe");
        var branch2 = result("Highlands Coffee", "cafe");
        var branch3 = result("Highlands Coffee", "cafe");

        var out = reranker.rerank(List.of(branch1, other, branch2, branch3),
                DiversityReranker.DEFAULT_CATEGORY_CAP, true);

        // Chỉ 1 chi nhánh Highlands ở đầu (branch1), quán khác tên được đôn lên,
        // hai chi nhánh còn lại bị đẩy xuống cuối.
        assertEquals(branch1, out.get(0));
        assertEquals(other, out.get(1));
        assertTrue(out.indexOf(branch2) > out.indexOf(other), "chi nhánh trùng phải xuống sau");
        assertTrue(out.indexOf(branch3) > out.indexOf(other), "chi nhánh trùng phải xuống sau");
    }

    @Test
    void gioi_han_so_quan_cung_loai() {
        // 5 quán loại "cafe" (tên khác nhau) xen 1 quán loại "food"; cap = 3.
        var c1 = result("Cafe 1", "cafe");
        var c2 = result("Cafe 2", "cafe");
        var c3 = result("Cafe 3", "cafe");
        var c4 = result("Cafe 4", "cafe");
        var food = result("Quán Ăn", "food");
        var c5 = result("Cafe 5", "cafe");

        var out = reranker.rerank(List.of(c1, c2, c3, c4, food, c5), 3, true);

        // Ba vị trí đầu chỉ được tối đa 3 quán "cafe".
        long cafeInTop3 = out.subList(0, 3).stream().filter(r -> r.category().equals("cafe")).count();
        assertTrue(cafeInTop3 <= 3, "không quá 3 quán cùng loại ở vùng đầu");
        // Quán loại khác được đôn lên trên các quán cafe dư (c4, c5).
        assertTrue(out.indexOf(food) < out.indexOf(c4), "quán loại khác phải nổi lên trước cafe dư");
        assertTrue(out.indexOf(food) < out.indexOf(c5), "quán loại khác phải nổi lên trước cafe dư");
    }

    @Test
    void khong_lam_mat_ket_qua() {
        var input = List.of(
                result("Highlands Coffee", "cafe"),
                result("Highlands Coffee", "cafe"),
                result("Phúc Long", "cafe"),
                result("Quán Ăn Ngon", "food"),
                result("Quán Ăn Ngon", "food"));

        var out = reranker.rerank(input, DiversityReranker.DEFAULT_CATEGORY_CAP, true);

        assertEquals(input.size(), out.size(), "không được mất kết quả nào");
        assertTrue(out.containsAll(input), "mọi kết quả gốc phải còn nguyên trong output");
    }

    private static SearchResult result(String name, String category) {
        return new SearchResult(UUID.randomUUID(), name, category, "địa chỉ",
                10.77, 106.70, 100.0, true, new ScoreDetail(0, 0, 0, 0, 0));
    }
}

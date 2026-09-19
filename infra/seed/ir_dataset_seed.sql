-- IR dataset seed — POI được thiết kế có chủ đích để đánh giá search engine nội bộ
-- (BM25 + spatial + time-aware + rating), độc lập với Geoapify.
--
-- Khác với demo_seed.sql (dữ liệu demo cho luồng auth/booking/payment/review), file này
-- CHỈ thêm POI — không đụng app_user/booking/payment/review — nên chạy an toàn, độc lập,
-- và có thể áp dụng lại nhiều lần (ON CONFLICT DO UPDATE) mà không phá dữ liệu demo hiện có.
--
-- Thiết kế theo yêu cầu dataset/evaluation (Phase 3):
--   3.1 keyword đa dạng: học bài, làm việc, yên tĩnh, wifi, ổ cắm, coffee, ăn sáng,
--       ăn tối, mở khuya, gia đình, take-away — trải đều trong description.
--   3.2 tên gần giống nhau: nhóm "ABC Coffee" (4 chi nhánh) để test diversity re-rank sau này.
--   3.3 giờ mở cửa qua đêm: một số POI 18:00→02:00 (hoặc biến thể), một số 08:00→22:00.
--   3.4 rating/reviewCount lệch nhau có chủ đích: 5.0/1, 4.9/500, 4.5/1000, 4.2/300.
--   3.5 cùng category nhưng khoảng cách khác nhau: 0.3 / 1 / 3 / 8 km từ một điểm tham chiếu
--       (10.7769, 106.7009 — trung tâm Quận 1, cùng khu vực với demo_seed.sql).
--
-- avg_rating/rating_count được set trực tiếp trên POI (không cần review row thật) vì đây là
-- tín hiệu ranking cần kiểm soát chính xác cho evaluation, không phải nội dung review hiển thị.

BEGIN;

CREATE TEMP TABLE ir_poi_data (
  seed_key TEXT PRIMARY KEY, owner_id UUID, category_id UUID, name TEXT, normalized_name TEXT,
  description TEXT, latitude DOUBLE PRECISION, longitude DOUBLE PRECISION, address TEXT,
  price_level INTEGER, capacity INTEGER, avg_rating NUMERIC(3,2), rating_count INTEGER,
  open_time TIME, close_time TIME, spans_next_day BOOLEAN
) ON COMMIT DROP;

-- Category id: ca-phe=...0001, nha-hang=...0002, tham-quan=...0003, luu-tru=...0004
-- Owner id: owner1=a1000000...0001, owner2=a2000000...0001 (xen kẽ, giống demo_seed.sql)

INSERT INTO ir_poi_data VALUES
  -- Group A — "ABC Coffee" tên gần giống nhau (3.2) + trải khoảng cách 0.3/1/3/8km (3.5).
  ('ir-01','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'ABC Coffee','abc coffee',
   'Quán cà phê yên tĩnh cho học sinh sinh viên học bài, có wifi mạnh và ổ cắm đầy đủ ở mọi bàn.',
   10.77923,106.70227,'10 Lê Lợi, Quận 1',2,40,4.80,1,'07:00','22:00',FALSE),
  ('ir-02','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'ABC Coffee 2','abc coffee 2',
   'Chi nhánh ABC Coffee mở khuya, phục vụ take-away nhanh gọn cho khách làm ca đêm.',
   10.78468,106.70547,'22 Nguyễn Trãi, Quận 1',2,35,4.20,300,'18:00','02:00',TRUE),
  ('ir-03','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'ABC Coffee Nguyễn Trãi','abc coffee nguyen trai',
   'Không gian rộng phù hợp gia đình vào buổi sáng, có set ăn sáng kèm cà phê giá tốt.',
   10.80024,106.71462,'156 Nguyễn Trãi, Quận 5',2,60,4.90,500,'07:00','22:00',FALSE),
  ('ir-04','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'ABC Coffee Landmark','abc coffee landmark',
   'Quán cà phê mở khuya trên cao, chỗ làm việc riêng tư và view landmark về đêm.',
   10.83914,106.73748,'720A Điện Biên Phủ, Bình Thạnh',3,50,4.50,1000,'18:00','02:00',TRUE),

  -- Group B — cụm quán học bài / làm việc (lõi cho truy vấn "cà phê học bài", "coffee làm việc").
  ('ir-05','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'Góc Học Đường Coffee','goc hoc duong coffee',
   'Quán cà phê yên tĩnh chuyên phục vụ học sinh sinh viên học bài, wifi tốc độ cao và ổ cắm mỗi bàn.',
   10.77680,106.69750,'8 Nguyễn Văn Chiêm, Quận 1',2,32,4.70,210,'07:00','22:00',FALSE),
  ('ir-06','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'Nhà Cà Phê Sách 2','nha ca phe sach 2',
   'Chi nhánh 2 của cà phê sách, không gian yên tĩnh để đọc sách và học bài buổi tối.',
   10.78900,106.69200,'19 Lý Chính Thắng, Quận 3',2,38,4.40,90,'08:00','22:00',FALSE),
  ('ir-07','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'Nhà Cà Phê Sách Landmark 81','nha ca phe sach landmark 81',
   'Quán cà phê sách trên cao view landmark, yên tĩnh, phù hợp làm việc và học bài cả ngày.',
   10.79470,106.72180,'Vinhomes Central Park, Bình Thạnh',3,45,4.60,140,'07:00','23:00',FALSE),
  ('ir-08','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'WorkHub Coffee & Coworking','workhub coffee coworking',
   'Không gian coworking kết hợp cà phê, làm việc cả ngày với wifi ổn định và nhiều ổ cắm điện.',
   10.77300,106.69200,'55 Cách Mạng Tháng 8, Quận 3',3,70,4.60,150,'07:00','23:00',FALSE),

  -- Group C — quán ăn sáng / ăn tối / gia đình / take-away.
  ('ir-09','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002',
   'Bữa Sáng Sài Gòn','bua sang sai gon',
   'Quán ăn sáng gia đình với bánh mì, phở, xôi phục vụ nhanh, không gian sạch sẽ thoáng mát.',
   10.77050,106.69850,'14 Trần Hưng Đạo, Quận 1',2,50,4.40,80,'06:00','11:00',FALSE),
  ('ir-10','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002',
   'Nhà Hàng Tối Muộn','nha hang toi muon',
   'Nhà hàng ăn tối mở khuya cho khách về trễ, thực đơn món Việt đa dạng phục vụ đến 1 giờ sáng.',
   10.76800,106.70600,'201 Nguyễn Thái Học, Quận 1',3,80,4.30,95,'17:00','01:00',TRUE),
  ('ir-11','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002',
   'Gia Đình Quán','gia dinh quan',
   'Quán ăn tối phù hợp gia đình, không gian rộng có khu vui chơi trẻ em, nhận đặt món take-away.',
   10.78600,106.68900,'33 Võ Thị Sáu, Quận 3',2,90,4.10,60,'10:00','22:00',FALSE),
  ('ir-12','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002',
   'Take Away Express','take away express',
   'Quán ăn nhanh chuyên take-away bữa sáng và bữa tối, đặt trước qua điện thoại lấy liền không chờ.',
   10.77450,106.71000,'9 Hai Bà Trưng, Quận 1',1,20,4.00,40,'06:00','22:00',FALSE),

  -- Group D — mở khuya (biên độ giờ khác nhau để test time-aware nhiều case hơn).
  ('ir-13','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'Đêm Sài Gòn Coffee','dem sai gon coffee',
   'Cà phê mở khuya tới sáng, phục vụ take-away cho dân văn phòng làm việc ca đêm và khách du lịch.',
   10.76500,106.69400,'77 Bùi Viện, Quận 1',2,45,4.60,220,'20:00','03:00',TRUE),
  ('ir-14','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002',
   'Phố Khuya Quán Ăn','pho khuya quan an',
   'Quán ăn tối mở khuya, không gian ngoài trời yên tĩnh, phù hợp nhóm bạn ăn khuya sau giờ làm.',
   10.76300,106.70200,'12 Đề Thám, Quận 1',2,55,4.20,130,'19:00','02:00',TRUE),

  -- Group E — cùng category "cà phê" nhưng khoảng cách khác nhau 0.3/1/3/8km (3.5, không trùng
  -- tên với group A để có bộ dữ liệu khoảng cách độc lập, dễ đối chiếu trong report).
  ('ir-15','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'Cà Phê Góc Phố','ca phe goc pho',
   'Quán cà phê nhỏ ngay góc phố trung tâm, yên tĩnh, phù hợp ngồi làm việc buổi sáng.',
   10.77825,106.69852,'3 Mạc Thị Bưởi, Quận 1',2,25,4.50,75,'07:00','22:00',FALSE),
  ('ir-16','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'Cà Phê Trung Tâm Mới','ca phe trung tam moi',
   'Cà phê phong cách hiện đại cách trung tâm khoảng 1km, có khu làm việc riêng và wifi miễn phí.',
   10.78139,106.69298,'45 Nguyễn Thị Diệu, Quận 3',2,40,4.30,110,'07:00','22:00',FALSE),
  ('ir-17','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'Cà Phê Xa Trung Tâm','ca phe xa trung tam',
   'Quán cà phê sân vườn cách trung tâm khoảng 3km, không gian rộng rãi, yên tĩnh và mát mẻ.',
   10.79037,106.67714,'120 Lê Văn Sỹ, Quận 3',1,60,4.10,50,'07:00','21:00',FALSE),
  ('ir-18','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'Cà Phê Ngoại Ô','ca phe ngoai o',
   'Cà phê vườn ở ngoại ô cách trung tâm khoảng 8km, thoáng mát, phù hợp gia đình cuối tuần.',
   10.81283,106.63755,'8 Quốc lộ 1A, Quận 12',1,80,4.00,35,'06:00','21:00',FALSE),

  -- Group F — rating/reviewCount lệch nhau có chủ đích (3.4, giá trị đúng như plan yêu cầu).
  ('ir-19','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'Quán 5 Sao Một Lượt','quan 5 sao mot luot',
   'Quán cà phê mới mở, yên tĩnh và sạch sẽ, mới có một lượt đánh giá 5 sao đầu tiên.',
   10.77200,106.70500,'6 Nguyễn Siêu, Quận 1',2,20,5.00,1,'07:00','22:00',FALSE),
  ('ir-20','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002',
   'Huyền Thoại 500 Đánh Giá','huyen thoai 500 danh gia',
   'Nhà hàng lâu năm nổi tiếng, không gian gia đình ấm cúng, đã có 500 lượt đánh giá 4.9 sao.',
   10.77900,106.70700,'40 Đồng Khởi, Quận 1',3,100,4.90,500,'10:00','22:00',FALSE),
  ('ir-21','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
   'Ngàn Lượt Tin Dùng','ngan luot tin dung',
   'Quán cà phê làm việc quen thuộc của dân văn phòng, wifi ổn định, đã có 1000 lượt đánh giá 4.5 sao.',
   10.77000,106.69600,'15 Nam Kỳ Khởi Nghĩa, Quận 1',2,55,4.50,1000,'07:00','22:00',FALSE),
  ('ir-22','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002',
   'Ổn Định 300 Đánh Giá','on dinh 300 danh gia',
   'Quán ăn tối bình dân, phục vụ nhanh, phù hợp gia đình, đã có 300 lượt đánh giá ổn định 4.2 sao.',
   10.78200,106.68600,'27 Bà Huyện Thanh Quan, Quận 3',2,65,4.20,300,'10:00','22:00',FALSE),

  -- Group G — tham quan / lưu trú (đa dạng category ngoài coffee/nhà hàng).
  ('ir-23','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000003',
   'Công Viên Ven Kênh','cong vien ven kenh',
   'Công viên ven kênh yên tĩnh, phù hợp gia đình dạo bộ buổi sáng và picnic cuối tuần.',
   10.76600,106.68800,'Kênh Nhiêu Lộc, Quận 3',1,300,4.30,180,'05:00','21:00',FALSE),
  ('ir-24','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000004',
   'Homestay Xanh Quận 4','homestay xanh quan 4',
   'Homestay yên tĩnh nhiều cây xanh, wifi miễn phí, phù hợp gia đình lưu trú ngắn ngày.',
   10.76100,106.70400,'21 Tôn Thất Thuyết, Quận 4',2,20,4.60,65,'00:00','23:59',FALSE),
  ('ir-25','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002',
   'Rooftop Ngắm Cảnh','rooftop ngam canh',
   'Nhà hàng rooftop ăn tối mở khuya, view thành phố về đêm, phù hợp nhóm bạn và cặp đôi.',
   10.77600,106.71300,'2 Hải Triều, Quận 1',4,70,4.70,240,'16:00','23:59:59',FALSE);

INSERT INTO poi(id, owner_id, category_id, name, normalized_name, description, latitude, longitude,
                location, address, price_level, capacity, booking_enabled, status, avg_rating,
                rating_count, created_at, updated_at)
SELECT md5(seed_key)::uuid, owner_id, category_id, name, normalized_name, description, latitude, longitude,
       ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography, address, price_level, capacity,
       TRUE, 'ACTIVE', avg_rating, rating_count, NOW(), NOW()
FROM ir_poi_data
ON CONFLICT (id) DO UPDATE SET
  owner_id=EXCLUDED.owner_id, category_id=EXCLUDED.category_id, name=EXCLUDED.name,
  normalized_name=EXCLUDED.normalized_name, description=EXCLUDED.description,
  latitude=EXCLUDED.latitude, longitude=EXCLUDED.longitude, address=EXCLUDED.address,
  price_level=EXCLUDED.price_level, capacity=EXCLUDED.capacity,
  booking_enabled=TRUE, status='ACTIVE', avg_rating=EXCLUDED.avg_rating,
  rating_count=EXCLUDED.rating_count, updated_at=NOW();

DELETE FROM poi_opening_hour WHERE poi_id IN (SELECT md5(seed_key)::uuid FROM ir_poi_data);
INSERT INTO poi_opening_hour(id, poi_id, day_of_week, open_time, close_time, closed, spans_next_day)
SELECT md5('ir-hours-' || d.seed_key || '-' || day_no)::uuid, md5(d.seed_key)::uuid, day_no,
       d.open_time, d.close_time, FALSE, d.spans_next_day
FROM ir_poi_data d CROSS JOIN generate_series(1,7) AS day_no;

INSERT INTO poi_approval_history(id, poi_id, admin_id, decision, reason, created_at)
SELECT md5('ir-approval-' || seed_key)::uuid, md5(seed_key)::uuid,
       'a0000000-0000-0000-0000-000000000001', 'APPROVED', 'IR dataset seed', NOW()
FROM ir_poi_data
ON CONFLICT (id) DO NOTHING;

INSERT INTO poi_search_document(poi_id, normalized_text, document_length, indexed_at)
SELECT p.id, lower(concat_ws(' ', p.name, p.description, p.address, c.name)),
       cardinality(regexp_split_to_array(trim(concat_ws(' ', p.name, p.description, p.address, c.name)), '\s+')), NOW()
FROM poi p JOIN category c ON c.id=p.category_id
WHERE p.id IN (SELECT md5(seed_key)::uuid FROM ir_poi_data)
ON CONFLICT (poi_id) DO UPDATE SET normalized_text=EXCLUDED.normalized_text,
  document_length=EXCLUDED.document_length, indexed_at=NOW();

COMMIT;

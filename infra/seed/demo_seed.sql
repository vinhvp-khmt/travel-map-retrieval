BEGIN;

-- Local/demo accounts only. Password for every account: Demo1234!
INSERT INTO app_user(id, email, password_hash, role, status, failed_attempts, created_at, updated_at) VALUES
  ('a0000000-0000-0000-0000-000000000001', 'admin@travelmap.local',  '$2a$10$pqyrm0/D2Z.MQhqHuzhekOyfBmUoiuZHlQAIW1enb0nSqTIBiiZQm', 'ADMIN', 'ACTIVE', 0, NOW(), NOW()),
  ('a1000000-0000-0000-0000-000000000001', 'owner1@travelmap.local', '$2a$10$pqyrm0/D2Z.MQhqHuzhekOyfBmUoiuZHlQAIW1enb0nSqTIBiiZQm', 'OWNER', 'ACTIVE', 0, NOW(), NOW()),
  ('a2000000-0000-0000-0000-000000000001', 'owner2@travelmap.local', '$2a$10$pqyrm0/D2Z.MQhqHuzhekOyfBmUoiuZHlQAIW1enb0nSqTIBiiZQm', 'OWNER', 'ACTIVE', 0, NOW(), NOW()),
  ('a3000000-0000-0000-0000-000000000001', 'user@travelmap.local',   '$2a$10$pqyrm0/D2Z.MQhqHuzhekOyfBmUoiuZHlQAIW1enb0nSqTIBiiZQm', 'USER',  'ACTIVE', 0, NOW(), NOW())
ON CONFLICT (email) DO UPDATE SET
  password_hash = EXCLUDED.password_hash, role = EXCLUDED.role, status = 'ACTIVE', updated_at = NOW();

CREATE TEMP TABLE demo_poi_data (
  seed_key TEXT PRIMARY KEY, owner_id UUID, category_id UUID, name TEXT, normalized_name TEXT,
  description TEXT, latitude DOUBLE PRECISION, longitude DOUBLE PRECISION, address TEXT,
  price_level INTEGER, capacity INTEGER
) ON COMMIT DROP;

INSERT INTO demo_poi_data VALUES
  ('poi-01','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','Cà phê Bến Thành','ca phe ben thanh','Cafe rang xay, không gian yên tĩnh gần chợ Bến Thành.',10.77210,106.69820,'12 Lê Thánh Tôn, Quận 1',2,40),
  ('poi-02','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','The Workshop Coffee','the workshop coffee','Specialty cafe với khu làm việc và cà phê pha thủ công.',10.77330,106.70410,'27 Ngô Đức Kế, Quận 1',3,55),
  ('poi-03','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','Cà phê Bờ Sông','ca phe bo song','Cafe thoáng mát nhìn ra sông Sài Gòn, phù hợp ngắm hoàng hôn.',10.78060,106.70870,'5B Tôn Đức Thắng, Quận 1',3,70),
  ('poi-04','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','Nhà Cà Phê Sách','nha ca phe sach','Cafe sách yên tĩnh với không gian đọc và làm việc.',10.78450,106.69510,'42 Nguyễn Đình Chiểu, Quận 3',2,35),
  ('poi-05','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','Cà phê Hẻm Xanh','ca phe hem xanh','Cafe sân vườn nhỏ, nhiều cây xanh giữa trung tâm thành phố.',10.76920,106.69030,'18/7 Nguyễn Thị Minh Khai, Quận 1',2,30),
  ('poi-06','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002','Bếp Việt Sài Gòn','bep viet sai gon','Nhà hàng món Việt truyền thống, phù hợp gia đình và nhóm bạn.',10.77540,106.70110,'88 Pasteur, Quận 1',3,100),
  ('poi-07','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002','Phở Trung Tâm','pho trung tam','Phở bò và món ăn sáng Việt Nam ngay trung tâm.',10.77110,106.69670,'25 Trương Định, Quận 1',2,60),
  ('poi-08','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002','Nhà hàng Sông Xanh','nha hang song xanh','Hải sản và món Việt với không gian nhìn ra sông.',10.78300,106.71120,'2 Nguyễn Hữu Cảnh, Bình Thạnh',4,120),
  ('poi-09','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002','Bún Chả Phố Nhỏ','bun cha pho nho','Bún chả, nem và món Hà Nội trong không gian ấm cúng.',10.77930,106.69360,'91 Võ Văn Tần, Quận 3',2,45),
  ('poi-10','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000002','Chay An Nhiên','chay an nhien','Nhà hàng chay hiện đại, nguyên liệu theo mùa.',10.78810,106.69720,'11 Trần Quốc Thảo, Quận 3',3,65),
  ('poi-11','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000003','Bảo tàng Thành phố','bao tang thanh pho','Điểm tham quan lịch sử và kiến trúc giữa trung tâm Sài Gòn.',10.77620,106.69940,'65 Lý Tự Trọng, Quận 1',1,200),
  ('poi-12','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000003','Dinh Độc Lập','dinh doc lap','Di tích lịch sử nổi tiếng với khuôn viên xanh rộng lớn.',10.77710,106.69530,'135 Nam Kỳ Khởi Nghĩa, Quận 1',2,300),
  ('poi-13','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000003','Bưu điện Trung tâm','buu dien trung tam','Công trình kiến trúc cổ cạnh Nhà thờ Đức Bà.',10.77980,106.69990,'2 Công xã Paris, Quận 1',1,250),
  ('poi-14','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000003','Phố đi bộ Nguyễn Huệ','pho di bo nguyen hue','Không gian đi bộ, sự kiện và ngắm cảnh về đêm.',10.77360,106.70340,'Nguyễn Huệ, Quận 1',1,500),
  ('poi-15','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000003','Thảo Cầm Viên','thao cam vien','Vườn thú và vườn thực vật lâu đời phù hợp gia đình.',10.78750,106.70530,'2 Nguyễn Bỉnh Khiêm, Quận 1',2,400),
  ('poi-16','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000004','Khách sạn Central Park','khach san central park','Khách sạn tiện nghi gần trung tâm và khu mua sắm.',10.77480,106.69600,'75 Lê Lai, Quận 1',3,80),
  ('poi-17','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000004','Saigon Riverside Stay','saigon riverside stay','Lưu trú hiện đại có tầm nhìn sông và trung tâm.',10.78120,106.70750,'17 Tôn Đức Thắng, Quận 1',4,110),
  ('poi-18','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000004','Nhà nghỉ Phố Cổ','nha nghi pho co','Lưu trú tiết kiệm gần chợ, thuận tiện đi bộ tham quan.',10.77000,106.69410,'43 Bùi Viện, Quận 1',1,35),
  ('poi-19','a1000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000004','Boutique Hotel 42','boutique hotel 42','Khách sạn boutique yên tĩnh với thiết kế địa phương.',10.78220,106.69240,'42 Tú Xương, Quận 3',3,45),
  ('poi-20','a2000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000004','Garden Home Saigon','garden home saigon','Không gian lưu trú nhiều cây xanh dành cho kỳ nghỉ ngắn.',10.79020,106.70100,'8 Nguyễn Văn Thủ, Quận 1',2,30);

INSERT INTO poi(id, owner_id, category_id, name, normalized_name, description, latitude, longitude,
                location, address, price_level, capacity, booking_enabled, status, avg_rating,
                rating_count, created_at, updated_at)
SELECT md5(seed_key)::uuid, owner_id, category_id, name, normalized_name, description, latitude, longitude,
       ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography, address, price_level, capacity,
       TRUE, 'ACTIVE', 0, 0, NOW(), NOW()
FROM demo_poi_data
ON CONFLICT (id) DO UPDATE SET
  owner_id=EXCLUDED.owner_id, category_id=EXCLUDED.category_id, name=EXCLUDED.name,
  normalized_name=EXCLUDED.normalized_name, description=EXCLUDED.description,
  latitude=EXCLUDED.latitude, longitude=EXCLUDED.longitude, address=EXCLUDED.address,
  price_level=EXCLUDED.price_level, capacity=EXCLUDED.capacity,
  booking_enabled=TRUE, status='ACTIVE', updated_at=NOW();

DELETE FROM poi_opening_hour WHERE poi_id IN (SELECT md5(seed_key)::uuid FROM demo_poi_data);
INSERT INTO poi_opening_hour(id, poi_id, day_of_week, open_time, close_time, closed, spans_next_day)
SELECT md5('hours-' || d.seed_key || '-' || day_no)::uuid, md5(d.seed_key)::uuid, day_no,
       CASE WHEN day_no = 7 AND d.category_id = '10000000-0000-0000-0000-000000000003' THEN NULL ELSE TIME '07:00' END,
       CASE WHEN day_no = 7 AND d.category_id = '10000000-0000-0000-0000-000000000003' THEN NULL ELSE TIME '22:00' END,
       day_no = 7 AND d.category_id = '10000000-0000-0000-0000-000000000003', FALSE
FROM demo_poi_data d CROSS JOIN generate_series(1,7) AS day_no;

INSERT INTO poi_approval_history(id, poi_id, admin_id, decision, reason, created_at)
SELECT md5('approval-' || seed_key)::uuid, md5(seed_key)::uuid,
       'a0000000-0000-0000-0000-000000000001', 'APPROVED', 'Local demo seed', NOW()
FROM demo_poi_data
ON CONFLICT (id) DO NOTHING;

INSERT INTO booking(id, user_id, poi_id, visit_at, slot_end_at, party_size, notes, status,
                    hold_expires_at, deposit_amount, currency, version, created_at, updated_at)
SELECT md5('booking-' || n)::uuid, 'a3000000-0000-0000-0000-000000000001',
       md5('poi-' || lpad(n::text, 2, '0'))::uuid,
       NOW() - (n || ' days')::interval, NOW() - (n || ' days')::interval + interval '1 hour',
       2 + (n % 3), 'Demo booking ' || n, 'COMPLETED', NULL, 50000 + n * 5000, 'VND', 0,
       NOW() - (n || ' days')::interval - interval '2 days', NOW() - (n || ' days')::interval
FROM generate_series(1,12) AS n
ON CONFLICT (id) DO UPDATE SET status='COMPLETED', hold_expires_at=NULL, updated_at=NOW();

INSERT INTO payment(id, booking_id, gateway, external_txn_id, idempotency_key, amount, currency,
                    status, payment_url, paid_at, created_at, updated_at)
SELECT md5('payment-' || n)::uuid, md5('booking-' || n)::uuid, 'MOCK', 'demo-txn-' || n,
       'demo-idempotency-' || n, 50000 + n * 5000, 'VND', 'PAID', NULL,
       NOW() - (n || ' days')::interval - interval '1 day', NOW() - (n || ' days')::interval - interval '2 days', NOW()
FROM generate_series(1,12) AS n
ON CONFLICT (id) DO UPDATE SET status='PAID', updated_at=NOW();

INSERT INTO review(id, booking_id, poi_id, user_id, rating, comment, status, editable_until, created_at, updated_at)
SELECT md5('review-' || n)::uuid, md5('booking-' || n)::uuid,
       md5('poi-' || lpad(n::text, 2, '0'))::uuid,
       'a3000000-0000-0000-0000-000000000001', 3 + (n % 3),
       CASE n % 3 WHEN 0 THEN 'Trải nghiệm rất tốt, sẽ quay lại.'
                  WHEN 1 THEN 'Vị trí thuận tiện và phục vụ thân thiện.'
                  ELSE 'Không gian đẹp, phù hợp chuyến đi cuối tuần.' END,
       'PUBLISHED', NOW() - (n || ' days')::interval + interval '1 day',
       NOW() - (n || ' days')::interval, NOW() - (n || ' days')::interval
FROM generate_series(1,12) AS n
ON CONFLICT (id) DO UPDATE SET rating=EXCLUDED.rating, comment=EXCLUDED.comment, status='PUBLISHED', updated_at=NOW();

UPDATE poi p SET avg_rating = stats.avg_rating, rating_count = stats.rating_count, updated_at = NOW()
FROM (
  SELECT p2.id, COALESCE(ROUND(AVG(r.rating), 2), 0) avg_rating, COUNT(r.id) rating_count
  FROM poi p2 LEFT JOIN review r ON r.poi_id=p2.id AND r.status='PUBLISHED'
  WHERE p2.id IN (SELECT md5(seed_key)::uuid FROM demo_poi_data)
  GROUP BY p2.id
) stats WHERE p.id=stats.id;

INSERT INTO poi_search_document(poi_id, normalized_text, document_length, indexed_at)
SELECT p.id, lower(concat_ws(' ', p.name, p.description, p.address, c.name)),
       cardinality(regexp_split_to_array(trim(concat_ws(' ', p.name, p.description, p.address, c.name)), '\s+')), NOW()
FROM poi p JOIN category c ON c.id=p.category_id
WHERE p.id IN (SELECT md5(seed_key)::uuid FROM demo_poi_data)
ON CONFLICT (poi_id) DO UPDATE SET normalized_text=EXCLUDED.normalized_text,
  document_length=EXCLUDED.document_length, indexed_at=NOW();

COMMIT;

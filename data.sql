INSERT INTO p_hub_stock (product_id, product_code, product_name, vendor_id, vendor_name, hub_id, quantity, version,
                         created_at, created_by, updated_at, updated_by, deleted_at, deleted_by)
VALUES
-- 업체 1: 삼성전자 (전자제품 위주)
('11ef6a7b-8c9d-4012-a345-67890abcdef1', 'SAM-001', '갤럭시 S24', '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a001', '삼성전자 물류센터',
 'a1b2c3d4-e5f6-7890-1234-567890abcdef', 100, 1, NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),

('11ef6a7b-8c9d-4012-a345-67890abcdef2', 'SAM-002', '갤럭시 북4', '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a001', '삼성전자 물류센터',
 'a1b2c3d4-e5f6-7890-1234-567890abcdef', 45, 1, NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),

-- 업체 1: 품절 상태 반영 (S9 태블릿)
('11ef6a7b-8c9d-4012-a345-67890abcdef3', 'SAM-003', '갤럭시 탭 S9', '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a001', '삼성전자 물류센터',
 'a1b2c3d4-e5f6-7890-1234-567890abcdef', 0, 1, NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),

-- 업체 2: 쿠팡 (생필품/대량 재고)
('22ef6a7b-8c9d-4012-a345-67890abcdef2', 'CP-002', '탐사수 2L', '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a002', '쿠팡 물류허브',
 'b2c3d4e5-f6a7-8901-2345-67890abcdef1', 1200, 1, NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),

-- 업체 3: CJ대한통운 (부자재)
('33ef6a7b-8c9d-4012-a345-67890abcdef2', 'CJ-002', '박스 테이프', '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a003', 'CJ대한통운 경기센터',
 'c3d4e5f6-a7b8-9012-3456-7890abcdef12', 300, 1, NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),

-- 업체 4: 마켓컬리 (신선식품/손실 가능성 높음)
('44ef6a7b-8c9d-4012-a345-67890abcdef1', 'KL-001', '유기농 우유', '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a004', '마켓컬리 신선센터',
 'd4e5f6a7-b8c9-0123-4567-890abcdef123', 85, 1, NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),

-- 업체 5: LG디스플레이 (고가 부품)
('55ef6a7b-8c9d-4012-a345-67890abcdef1', 'LG-001', 'OLED 패널 55인치', '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a005', 'LG디스플레이 공장',
 'e5f6a7b8-c9d0-1234-5678-90abcdef1234', 20, 1, NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),

-- 소프트 딜리트된 상태 (단종된 상품 예시)
('55ef6a7b-8c9d-4012-a345-67890abcdef5', 'LG-005', '노트북용 패널', '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a005', 'LG디스플레이 공장',
 'e5f6a7b8-c9d0-1234-5678-90abcdef1234', 0, 1, NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11');


-- 3. [p_hub_stock_history] 재고 변동 이력
INSERT INTO p_hub_stock_history (history_id, product_id, product_code, product_name, vendor_id, vendor_name, hub_id,
                                 type, ref_id, before_quantity, after_quantity,
                                 created_at, created_by, updated_at, updated_by, deleted_at, deleted_by)
VALUES
-- [삼성전자 S24] 초기 입고 150 -> 출고 50 -> 현재 100
(gen_random_uuid(), '11ef6a7b-8c9d-4012-a345-67890abcdef1', 'SAM-001', '갤럭시 S24',
 '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a001', '삼성전자 물류센터', 'a1b2c3d4-e5f6-7890-1234-567890abcdef', 'INBOUND', NULL, 0, 150,
 NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),
(gen_random_uuid(), '11ef6a7b-8c9d-4012-a345-67890abcdef1', 'SAM-001', '갤럭시 S24',
 '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a001', '삼성전자 물류센터', 'a1b2c3d4-e5f6-7890-1234-567890abcdef', 'OUTBOUND',
 gen_random_uuid(), 150, 100, NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),

-- [삼성전자 탭 S9] 입고 10 -> 전량 출고(품절) 10 -> 현재 0
(gen_random_uuid(), '11ef6a7b-8c9d-4012-a345-67890abcdef3', 'SAM-003', '갤럭시 탭 S9',
 '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a001', '삼성전자 물류센터', 'a1b2c3d4-e5f6-7890-1234-567890abcdef', 'INBOUND', NULL, 0, 10,
 NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),
(gen_random_uuid(), '11ef6a7b-8c9d-4012-a345-67890abcdef3', 'SAM-003', '갤럭시 탭 S9',
 '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a001', '삼성전자 물류센터', 'a1b2c3d4-e5f6-7890-1234-567890abcdef', 'OUTBOUND',
 gen_random_uuid(), 10, 0, NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),

-- [마켓컬리 우유] 입고 100 -> 파손으로 인한 조정(ADJUST_LOSS) 15 -> 현재 85
(gen_random_uuid(), '44ef6a7b-8c9d-4012-a345-67890abcdef1', 'KL-001', '유기농 우유',
 '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a004', '마켓컬리 신선센터', 'd4e5f6a7-b8c9-0123-4567-890abcdef123', 'INBOUND', NULL, 0, 100,
 NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),
(gen_random_uuid(), '44ef6a7b-8c9d-4012-a345-67890abcdef1', 'KL-001', '유기농 우유',
 '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a004', '마켓컬리 신선센터', 'd4e5f6a7-b8c9-0123-4567-890abcdef123', 'ADJUST_LOSS', NULL, 100,
 85, NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL),

-- [쿠팡 탐사수] 대량 입고
(gen_random_uuid(), '22ef6a7b-8c9d-4012-a345-67890abcdef2', 'CP-002', '탐사수 2L',
 '44d6507c-9b8e-4a6c-9f8d-4e92b3a1a002', '쿠팡 물류허브', 'b2c3d4e5-f6a7-8901-2345-67890abcdef1', 'INBOUND', NULL, 0, 1200,
 NOW(),
 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NOW(), 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', NULL, NULL);
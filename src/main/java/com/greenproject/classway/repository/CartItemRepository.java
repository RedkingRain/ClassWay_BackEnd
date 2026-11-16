package com.greenproject.classway.repository;

import com.greenproject.classway.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    /**
     * Native Query 결과 매핑용 인터페이스
     */
    interface CartItemRow {
        Integer getCartItemId();
        Integer getClassId();
        Integer getScheduleId();
        Integer getQuantity();
        String getTitle();
        String getThumbnailUrl();
        String getClassType();
        Integer getPrice();
        Integer getSalePrice();
        Integer getSaleRate();
        String getClassStatus();
        String getRegion();
        String getVodCnt();
        Integer getCapacity();
        String getScheduleStatus();
        Integer getOrderCapacity();
        String getStartAt();
    }

    /**
     * 특정 회원의 장바구니 목록과 클래스/스케줄 상세 정보를 조회
     */
    @Query(value = """
        SELECT 
            ci.cart_item_id AS cartItemId,
            ci.class_id AS classId,
            ci.schedule_id AS scheduleId,
            ci.quantity AS quantity,
            c.title AS title,
            c.thumbnail_url AS thumbnailUrl,
            c.class_type AS classType,
            c.price AS price,
            COALESCE(c.sale_price, c.price) AS salePrice,
            CASE 
                WHEN c.price > 0 AND c.sale_price IS NOT NULL 
                THEN ROUND((c.price - c.sale_price) / c.price * 100) 
                ELSE 0 
            END AS saleRate,
            c.status AS classStatus,
            CASE 
                WHEN c.class_type = 'ONLINE' THEN '수강 기한 없음' 
                ELSE c.region 
            END AS region,
            CASE 
                WHEN c.class_type = 'ONLINE' 
                THEN (SELECT CONCAT('VOD ', COUNT(cc.chapter_id), '강') FROM class_chapters cc WHERE cc.class_id = c.class_id)
                ELSE NULL 
            END AS vodCnt,
            cs.capacity AS capacity,
            cs.status AS scheduleStatus,
            COALESCE(
                (SELECT SUM(oi.quantity) 
                 FROM order_items oi 
                 WHERE oi.status IN ('PENDING', 'PAID') 
                   AND oi.class_id = ci.class_id 
                   AND oi.schedule_id = ci.schedule_id), 0
            ) AS orderCapacity,
            CASE 
                WHEN cs.start_at IS NOT NULL THEN
                    CONCAT(
                        DATE_FORMAT(cs.start_at, '%Y.%m.%d ('),
                        ELT(DAYOFWEEK(cs.start_at), '일', '월', '화', '수', '목', '금', '토'),
                        DATE_FORMAT(cs.start_at, ') %H:%i')
                    )
                ELSE NULL
            END AS startAt
        FROM cart_items ci
        JOIN classes c ON ci.class_id = c.class_id
        LEFT JOIN class_schedules cs ON ci.schedule_id = cs.schedule_id
        WHERE ci.user_id = :userId
        ORDER BY ci.cart_item_id DESC
    """, nativeQuery = true)
    List<CartItemRow> findCartItemsByUserId(@Param("userId") Integer userId);
}

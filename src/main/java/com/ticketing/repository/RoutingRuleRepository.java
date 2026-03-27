package com.ticketing.repository;
import com.ticketing.domain.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, String> {
    List<RoutingRule> findByActiveTrueOrderByPriorityAsc();
}

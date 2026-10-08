package com.iot.repository;

import com.iot.model.Device;
import com.iot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// 设备数据访问层：继承 JpaRepository 后自动拥有增删改查(save/findById/delete等)能力
public interface DeviceRepository extends JpaRepository<Device, Long> {
    // 按所属用户查询其名下所有设备（方法名由Spring Data JPA自动解析成SQL）
    List<Device> findByOwner(User owner);
}

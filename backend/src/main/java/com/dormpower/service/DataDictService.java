package com.dormpower.service;

import com.dormpower.model.DataDict;
import com.dormpower.repository.DataDictRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据字典服务
 */
@Service
public class DataDictService {

    @Autowired
    private DataDictRepository dataDictRepository;

    /**
     * 创建字典项
     */
    public DataDict createDict(DataDict dict) {
        if (dataDictRepository.existsByDictCode(dict.getDictCode())) {
            throw new RuntimeException("Dict code already exists: " + dict.getDictCode());
        }
        dict.setCreatedAt(System.currentTimeMillis() / 1000);
        dict.setUpdatedAt(System.currentTimeMillis() / 1000);
        return dataDictRepository.save(dict);
    }

    /**
     * 批量创建字典项
     */
    @Transactional
    public List<DataDict> batchCreateDict(List<DataDict> dicts) {
        List<DataDict> result = new ArrayList<>();
        for (DataDict dict : dicts) {
            if (!dataDictRepository.existsByDictCode(dict.getDictCode())) {
                dict.setCreatedAt(System.currentTimeMillis() / 1000);
                dict.setUpdatedAt(System.currentTimeMillis() / 1000);
                result.add(dataDictRepository.save(dict));
            }
        }
        return result;
    }

    /**
     * 更新字典项
     */
    public DataDict updateDict(Long id, DataDict dict) {
        DataDict existing = dataDictRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dict not found: " + id));

        existing.setDictLabel(dict.getDictLabel());
        existing.setDictValue(dict.getDictValue());
        existing.setParentCode(dict.getParentCode());
        existing.setSort(dict.getSort());
        existing.setDescription(dict.getDescription());
        existing.setEnabled(dict.isEnabled());
        existing.setDefault(dict.isDefault());
        existing.setCssClass(dict.getCssClass());
        existing.setListClass(dict.getListClass());
        existing.setUpdatedAt(System.currentTimeMillis() / 1000);

        return dataDictRepository.save(existing);
    }

    /**
     * 删除字典项
     */
    public void deleteDict(Long id) {
        DataDict dict = dataDictRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dict not found: " + id));
        
        if (dict.isSystem()) {
            throw new RuntimeException("Cannot delete system dict: " + dict.getDictCode());
        }
        
        dataDictRepository.deleteById(id);
    }

    /**
     * 获取字典项
     */
    public Optional<DataDict> getDictById(Long id) {
        return dataDictRepository.findById(id);
    }

    /**
     * 根据编码获取字典项
     */
    public Optional<DataDict> getDictByCode(String dictCode) {
        return dataDictRepository.findByDictCode(dictCode);
    }

    /**
     * 获取字典类型下的所有字典项
     */
    public List<DataDict> getDictsByType(String dictType) {
        return dataDictRepository.findByDictTypeOrderBySortAsc(dictType);
    }

    /**
     * 获取字典类型下的启用字典项
     */
    public List<DataDict> getEnabledDictsByType(String dictType) {
        return dataDictRepository.findByDictTypeAndEnabledTrueOrderBySortAsc(dictType);
    }

    /**
     * 获取所有字典类型
     */
    public List<String> getAllDictTypes() {
        return dataDictRepository.findAllDictTypes();
    }

    /**
     * 获取字典树形结构
     */
    public List<Map<String, Object>> getDictTree(String dictType) {
        List<DataDict> dicts = dataDictRepository.findByDictTypeOrderBySortAsc(dictType);
        return buildTree(dicts, null);
    }

    private List<Map<String, Object>> buildTree(List<DataDict> dicts, String parentCode) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DataDict dict : dicts) {
            boolean isChild = (parentCode == null && dict.getParentCode() == null) ||
                             (parentCode != null && parentCode.equals(dict.getParentCode()));
            if (isChild) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", dict.getId());
                node.put("dictCode", dict.getDictCode());
                node.put("dictLabel", dict.getDictLabel());
                node.put("dictValue", dict.getDictValue());
                node.put("sort", dict.getSort());
                node.put("enabled", dict.isEnabled());
                node.put("isDefault", dict.isDefault());
                node.put("children", buildTree(dicts, dict.getDictCode()));
                result.add(node);
            }
        }
        return result;
    }

    /**
     * 获取字典项标签
     */
    public String getDictLabel(String dictType, String dictCode) {
        return dataDictRepository.findByDictTypeAndDictCode(dictType, dictCode)
                .map(DataDict::getDictLabel)
                .orElse(dictCode);
    }

    /**
     * 获取字典项值
     */
    public String getDictValue(String dictType, String dictCode) {
        return dataDictRepository.findByDictTypeAndDictCode(dictType, dictCode)
                .map(DataDict::getDictValue)
                .orElse(null);
    }

    /**
     * 获取默认字典项
     */
    public Optional<DataDict> getDefaultDict(String dictType) {
        return dataDictRepository.findByDictTypeAndIsDefaultTrue(dictType);
    }

    /**
     * 分页查询
     */
    public Page<DataDict> getDictsByType(String dictType, Pageable pageable) {
        return dataDictRepository.findByDictType(dictType, pageable);
    }

    /**
     * 初始化系统字典
     */
    public void initSystemDicts() {
        String[][] dicts = {
            {"BILL_STATUS", "PENDING", "待缴费", "1"},
            {"BILL_STATUS", "PAID", "已缴费", "2"},
            {"BILL_STATUS", "OVERDUE", "已逾期", "3"},
            {"BILL_STATUS", "CANCELLED", "已取消", "4"},
            {"DEVICE_STATUS", "ONLINE", "在线", "1"},
            {"DEVICE_STATUS", "OFFLINE", "离线", "2"},
            {"DEVICE_STATUS", "FAULT", "故障", "3"},
            {"ALERT_LEVEL", "info", "信息", "1"},
            {"ALERT_LEVEL", "warning", "警告", "2"},
            {"ALERT_LEVEL", "error", "错误", "3"},
            {"ALERT_LEVEL", "critical", "严重", "4"},
            {"ALERT_STATUS", "ACTIVE", "活动", "1"},
            {"ALERT_STATUS", "ACKNOWLEDGED", "已确认", "2"},
            {"ALERT_STATUS", "RESOLVED", "已解决", "3"},
            {"PRICE_TYPE", "TIER", "阶梯电价", "1"},
            {"PRICE_TYPE", "TIME", "分时电价", "2"},
            {"PRICE_TYPE", "MIXED", "混合电价", "3"},
            {"NOTIFICATION_TYPE", "EMAIL", "邮件通知", "1"},
            {"NOTIFICATION_TYPE", "SYSTEM", "系统通知", "2"},
            {"NOTIFICATION_TYPE", "SMS", "短信通知", "3"},
            {"COLLECTION_STATUS", "PENDING", "待发送", "1"},
            {"COLLECTION_STATUS", "SENT", "已发送", "2"},
            {"COLLECTION_STATUS", "FAILED", "发送失败", "3"},
            {"IP_CONTROL_TYPE", "WHITELIST", "白名单", "1"},
            {"IP_CONTROL_TYPE", "BLACKLIST", "黑名单", "2"},
            {"STUDENT_STATUS", "ACTIVE", "在校", "1"},
            {"STUDENT_STATUS", "GRADUATED", "毕业", "2"},
            {"STUDENT_STATUS", "SUSPENDED", "休学", "3"},
            {"STUDENT_STATUS", "DROPPED", "退学", "4"},
            {"FIRMWARE_STATUS", "PENDING", "待升级", "1"},
            {"FIRMWARE_STATUS", "DOWNLOADING", "下载中", "2"},
            {"FIRMWARE_STATUS", "INSTALLING", "安装中", "3"},
            {"FIRMWARE_STATUS", "SUCCESS", "成功", "4"},
            {"FIRMWARE_STATUS", "FAILED", "失败", "5"},
        };

        for (String[] dict : dicts) {
            try {
                if (!dataDictRepository.existsByDictCode(dict[1])) {
                    DataDict dataDict = createSystemDict(dict[0], dict[1], dict[2], dict[3]);
                    dataDictRepository.save(dataDict);
                }
            } catch (Exception e) {
                // 忽略重复键错误
            }
        }
    }

    private DataDict createSystemDict(String type, String code, String label, String value) {
        DataDict dict = new DataDict(type, code, label);
        dict.setDictValue(value);
        dict.setSystem(true);
        dict.setEnabled(true);
        return dict;
    }
}

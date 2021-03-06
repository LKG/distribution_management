package com.stylefeng.guns.modular.dist.controller;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.stylefeng.guns.common.annotion.DataSource;
import com.stylefeng.guns.common.annotion.Permission;
import com.stylefeng.guns.common.annotion.log.BussinessLog;
import com.stylefeng.guns.common.constant.Const;
import com.stylefeng.guns.common.constant.DSEnum;
import com.stylefeng.guns.common.constant.Dict;
import com.stylefeng.guns.common.constant.factory.ConstantFactory;
import com.stylefeng.guns.common.controller.BaseController;
import com.stylefeng.guns.common.exception.BizExceptionEnum;
import com.stylefeng.guns.common.exception.BussinessException;
import com.stylefeng.guns.common.persistence.dao.SysDicMapper;
import com.stylefeng.guns.common.persistence.dao.SysDicTypeMapper;
import com.stylefeng.guns.common.persistence.model.SysDic;
import com.stylefeng.guns.common.persistence.model.SysDicType;
import com.stylefeng.guns.core.log.LogObjectHolder;
import com.stylefeng.guns.core.util.ToolUtil;
import com.stylefeng.guns.modular.system.dao.SysDicTypeDao;
import com.stylefeng.guns.modular.dist.service.ISysDicService;
import com.stylefeng.guns.modular.system.warpper.DicWarpper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author huangpu
 */
@Controller
@RequestMapping("/dic")
public class DicController extends BaseController {

    private String prefix = "/dist/dic/";

    @Resource
    SysDicTypeMapper sysDicTypeMapper;

    @Resource
    SysDicMapper sysDicMapper;

    @Resource
    SysDicTypeDao sysDicTypeDao;

    @Autowired
    ISysDicService sysDicService;



    /**
     * ???????????????????????????
     */
    @RequestMapping("")
    public String index() {
        return prefix + "dic.html";
    }


    /**
     * ????????????????????????
     */
    @RequestMapping(value = "/list")
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    @DataSource(name = DSEnum.DATA_SOURCE_BIZ)
    public Object list(String dicTypeName) {
        List<Map<String, Object>> list = this.sysDicTypeDao.list(dicTypeName);
        return super.warpObject(new DicWarpper(list));
    }

    /**
     * ?????????????????????
     */
    @RequestMapping("/dic_add")
    public String dicAdd() {
        return prefix + "dic_add.html";
    }
    /**
     * ????????????
     *
     * @param dictValues ????????????   "1:??????;2:??????;3:??????"
     */

    @BussinessLog(value = "????????????????????????", key = "dictName,dictValues", dict = Dict.DICT_MAP)
    @RequestMapping(value = "/add")
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public Object add(String dictName,String disTypeNo, String dictValues) {
        if (ToolUtil.isOneEmpty(dictName, dictValues)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        this.sysDicService.addDict(dictName,disTypeNo, dictValues);
        return SUCCESS_TIP;
    }

    /**
     * ?????????????????????
     */
    @Permission(Const.ADMIN_NAME)
    @RequestMapping("/dic_edit/{dictId}")
    @DataSource(name = DSEnum.DATA_SOURCE_BIZ)
    public String deptUpdate(@PathVariable Integer dictId, Model model) {
        SysDicType dict = sysDicTypeMapper.selectById(dictId);
        model.addAttribute("dict", dict);
        List<SysDic> subDicts = sysDicMapper.selectList(new EntityWrapper<SysDic>().eq("dic_type_no", dict.getDicTypeNo()));
        model.addAttribute("subDicts", subDicts);
        LogObjectHolder.me().set(dict);
        return prefix + "dic_edit.html";
    }

    /**
     * ????????????
     */
    @BussinessLog(value = "????????????", key = "dictName,dictValues", dict = Dict.DICT_MAP)
    @RequestMapping(value = "/update")
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public Object update(Integer dictId, String dictName,String disTypeNo, String dictValues) {
        if (ToolUtil.isOneEmpty(dictId, dictName, dictValues)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        sysDicService.editDict(dictId, dictName,disTypeNo, dictValues);
        return SUCCESS_TIP;
    }

    /**
     * ??????????????????
     */
    @BussinessLog(value = "??????????????????", key = "dictId", dict = Dict.DELETE_DICT)
    @RequestMapping(value = "/delete")
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public Object delete(@RequestParam Integer dictId) {
        //????????????????????????
        LogObjectHolder.me().set(ConstantFactory.me().getDictName(dictId));
        this.sysDicService.delteDict(dictId);
        return SUCCESS_TIP;
    }
}

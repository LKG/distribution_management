package com.stylefeng.guns.modular.system.controller;

import com.stylefeng.guns.common.annotion.Permission;
import com.stylefeng.guns.common.annotion.log.BussinessLog;
import com.stylefeng.guns.common.constant.Const;
import com.stylefeng.guns.common.constant.Dict;
import com.stylefeng.guns.common.constant.dist.*;
import com.stylefeng.guns.common.constant.factory.ConstantFactory;
import com.stylefeng.guns.common.constant.state.ManagerStatus;
import com.stylefeng.guns.common.constant.tips.AbstractTip;
import com.stylefeng.guns.common.controller.BaseController;
import com.stylefeng.guns.common.exception.BizExceptionEnum;
import com.stylefeng.guns.common.exception.BussinessException;
import com.stylefeng.guns.common.persistence.dao.UserMapper;
import com.stylefeng.guns.common.persistence.model.DisMemberInfo;
import com.stylefeng.guns.common.persistence.model.User;
import com.stylefeng.guns.config.properties.GunsProperties;
import com.stylefeng.guns.core.db.Db;
import com.stylefeng.guns.core.log.LogObjectHolder;
import com.stylefeng.guns.core.shiro.ShiroKit;
import com.stylefeng.guns.core.shiro.ShiroUser;
import com.stylefeng.guns.core.util.ToolUtil;
import com.stylefeng.guns.modular.dist.service.IDisMemberInfoService;
import com.stylefeng.guns.modular.system.dao.UserMgrDao;
import com.stylefeng.guns.modular.system.factory.UserFactory;
import com.stylefeng.guns.modular.dist.service.ISysDicService;
import com.stylefeng.guns.modular.system.transfer.UserDto;
import com.stylefeng.guns.modular.system.warpper.UserWarpper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.naming.NoPermissionException;
import javax.validation.Valid;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ????????????????????????
 *
 * @author fengshuonan
 * @Date 2017???1???11??? ??????1:08:17
 */
@Controller
@RequestMapping("/mgr")
public class UserMgrController extends BaseController {

    private static String PREFIX = "/system/user/";

    @Resource
    private GunsProperties gunsProperties;

    @Resource
    private UserMgrDao managerDao;

    @Resource
    private UserMapper userMapper;

    @Autowired
    ISysDicService sysDicService;

    @Autowired
    IDisMemberInfoService disMemberInfoService;




    /**
     * ?????????????????????
     */
    @Value("${dist.member.suffix}")
    private  String suffix;

    /**
     * ???????????????????????????????????????
     */
    @RequestMapping("")
    public String index() {
        return PREFIX + "user.html";
    }

    /**
     * ???????????????????????????????????????
     */
    @RequestMapping("/user_add")
    public String addView() {
        return PREFIX + "user_add.html";
    }

    /**
     * ???????????????????????????
     */
    @Permission
    @RequestMapping("/role_assign/{userId}")
    public String roleAssign(@PathVariable Integer userId, Model model) {
        if (ToolUtil.isEmpty(userId)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        User user = (User) Db.create(UserMapper.class).selectOneByCon("id", userId);
        model.addAttribute("userId", userId);
        model.addAttribute("userAccount", user.getAccount());
        return PREFIX + "user_roleassign.html";
    }

    /**
     * ??????????????????????????????
     */
    @Permission
    @RequestMapping("/user_edit/{userId}")
    public String userEdit(@PathVariable Integer userId, Model model) {
        if (ToolUtil.isEmpty(userId)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        User user = this.userMapper.selectById(userId);
        model.addAttribute(user);
        model.addAttribute("roleName", ConstantFactory.me().getRoleName(user.getRoleid()));
        model.addAttribute("deptName", ConstantFactory.me().getDeptName(user.getDeptid()));
        LogObjectHolder.me().set(user);
        return PREFIX + "user_edit.html";
    }

    /**
     * ?????????????????????????????????
     */
    @RequestMapping("/user_info")
    public String userInfo(Model model) {
        Integer userId = ShiroKit.getUser().getId();
        if (ToolUtil.isEmpty(userId)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        User user = this.userMapper.selectById(userId);
        model.addAttribute(user);
        model.addAttribute("roleName", ConstantFactory.me().getRoleName(user.getRoleid()));
        model.addAttribute("deptName", ConstantFactory.me().getDeptName(user.getDeptid()));
        LogObjectHolder.me().set(user);
        return PREFIX + "user_view.html";
    }

    /**
     * ???????????????????????????
     */
    @RequestMapping("/user_chpwd")
    public String chPwd() {
        return PREFIX + "user_chpwd.html";
    }

    /**
     * ???????????????????????????
     */
    @RequestMapping("/changePwd")
    @ResponseBody
    public Object changePwd(@RequestParam String oldPwd, @RequestParam String newPwd, @RequestParam String rePwd) {
        if (!newPwd.equals(rePwd)) {
            throw new BussinessException(BizExceptionEnum.TWO_PWD_NOT_MATCH);
        }
        Integer userId = ShiroKit.getUser().getId();
        User user = userMapper.selectById(userId);
        String oldMd5 = ShiroKit.md5(oldPwd, user.getSalt());
        if (user.getPassword().equals(oldMd5)) {
            String newMd5 = ShiroKit.md5(newPwd, user.getSalt());
            user.setPassword(newMd5);
            user.updateById();
            return SUCCESS_TIP;
        } else {
            throw new BussinessException(BizExceptionEnum.OLD_PWD_NOT_RIGHT);
        }
    }

    /**
     * ?????????????????????
     */
    @RequestMapping("/list")
    @Permission
    @ResponseBody
    public Object list(@RequestParam(required = false) String name, @RequestParam(required = false) String beginTime, @RequestParam(required = false) String endTime, @RequestParam(required = false) Integer deptid) {
        String account= ShiroKit.getUser().getAccount();
        String superAccount="";
        if(!SystemUser.ADMIN_INFO.getInfo().equals(account)){
            superAccount=account;
        }
        List<Map<String, Object>> users = managerDao.selectUsers(name, beginTime, endTime, deptid,superAccount);
        return new UserWarpper(users).warp();
    }

    /**
     * ???????????????
     */
    @RequestMapping("/add")
    @BussinessLog(value = "???????????????", key = "account", dict = Dict.USER_DICT)
    @ResponseBody
    public AbstractTip add(@Valid UserDto user, BindingResult result) {
        if (result.hasErrors()) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }

        // ????????????????????????
        User theUser = managerDao.getByAccount(user.getAccount());
        if (theUser != null) {
            throw new BussinessException(BizExceptionEnum.USER_ALREADY_REG);
        }
        String account= ShiroKit.getUser().getAccount();
        User currentUser= managerDao.getByAccount(account);
        Integer level= Integer.parseInt(currentUser.getLevel())+1;
        // ??????????????????
        user.setSalt(ShiroKit.getRandomSalt(5));
        user.setPassword(ShiroKit.md5(user.getPassword(), user.getSalt()));
        user.setStatus(ManagerStatus.OK.getCode());
        user.setCreatetime(new Date());
        user.setSuperaccount(account);
        user.setFullindex(currentUser.getFullindex()+"."+user.getAccount());
        user.setLevel(level.toString());
        //??????????????????roleID
        user.setRoleid(JurisdictionStatus.getMethod(level.toString()).getRoleId());
        this.userMapper.insert(UserFactory.createUser(user));

        DisMemberInfo memberInfo=new DisMemberInfo();
        memberInfo.setDisUserId(user.getAccount());
        memberInfo.setDisUserName(user.getName());
        memberInfo.setIdentityType(IdentityStatus.PLAT_STATUS.getStatus());
        memberInfo.setDisPlatformId(user.getFullindex().split("\\.")[1]);
        memberInfo.setDisPlatSuper(user.getSuperaccount());
        memberInfo.setDisPlatLevel(Integer.parseInt(user.getLevel()));
        memberInfo.setDisPlatFullIndex(user.getFullindex());
        memberInfo.setDisFullIndex(user.getFullindex());
        memberInfo.setDisParentId(user.getSuperaccount());
        memberInfo.setDisUserType(UserTypeStatus.PLAT_STATUS.getStatus());
//        memberInfo.setDisLevel(Integer.parseInt(user.getLevel()));
        memberInfo.setIsDelete("N");
        memberInfo.setDisUserRank(AgentRankStatus.A_STATUS.getStatus());
        disMemberInfoService.saveAgent(memberInfo);
        //??????????????????????????????
        /*if(DistCommonArg.ADMIN.equals(memberInfo.getDisParentId())){

        }*/

        String memberId =memberInfo.getDisUserId()+suffix;
        memberInfo.setDisParentId(null);
        memberInfo.setDisFullIndex(memberId);
        memberInfo.setDisUserId(memberId);
        memberInfo.setDisUserRank(UserRankStatus.A_STATUS.getStatus());
        memberInfo.setDisUserType(UserTypeStatus.ZERO_STATUS.getStatus());
        memberInfo.setDisNote("??????????????????????????????????????????");
        memberInfo.setIdentityType(IdentityStatus.USER_STATUS.getStatus());
        memberInfo.setDisUserRank(UserRankStatus.A_STATUS.getStatus());
        memberInfo.setDisPlatSuper(user.getAccount());
        disMemberInfoService.saveNoOperate(memberInfo);
        return SUCCESS_TIP;
    }

    /**
     * ???????????????
     *
     * @throws NoPermissionException
     */
    @RequestMapping("/edit")
    @BussinessLog(value = "???????????????", key = "account", dict = Dict.USER_DICT)
    @ResponseBody
    public AbstractTip edit(@Valid UserDto user, BindingResult result) throws NoPermissionException {
        if (result.hasErrors()) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        if (ShiroKit.hasRole(Const.ADMIN_NAME)) {
            this.userMapper.updateById(UserFactory.createUser(user));
            return SUCCESS_TIP;
        } else {
            ShiroUser shiroUser = ShiroKit.getUser();
            if (shiroUser.getId().equals(user.getId())) {
                this.userMapper.updateById(UserFactory.createUser(user));
                return SUCCESS_TIP;
            } else {
                throw new BussinessException(BizExceptionEnum.NO_PERMITION);
            }
        }
    }

    /**
     * ?????????????????????????????????
     */
    @RequestMapping("/delete")
    @BussinessLog(value = "???????????????", key = "userId", dict = Dict.USER_DICT)
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public AbstractTip delete(@RequestParam Integer userId) {
        if (ToolUtil.isEmpty(userId)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        //???????????????????????????
        if (userId.equals(Const.ADMIN_ID)) {
            throw new BussinessException(BizExceptionEnum.CANT_DELETE_ADMIN);
        }
        this.managerDao.setStatus(userId, ManagerStatus.DELETED.getCode());
        return SUCCESS_TIP;
    }

    /**
     * ?????????????????????
     */
    @RequestMapping("/view/{userId}")
    @ResponseBody
    public User view(@PathVariable Integer userId) {
        if (ToolUtil.isEmpty(userId)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        return this.userMapper.selectById(userId);
    }

    /**
     * ????????????????????????
     */
    @RequestMapping("/reset")
    @BussinessLog(value = "?????????????????????", key = "userId", dict = Dict.USER_DICT)
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public AbstractTip reset(@RequestParam Integer userId) {
        if (ToolUtil.isEmpty(userId)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        User user = this.userMapper.selectById(userId);
        user.setSalt(ShiroKit.getRandomSalt(5));
        user.setPassword(ShiroKit.md5(Const.DEFAULT_PWD, user.getSalt()));
        this.userMapper.updateById(user);
        return SUCCESS_TIP;
    }

    /**
     * ????????????
     */
    @RequestMapping("/freeze")
    @BussinessLog(value = "????????????", key = "userId", dict = Dict.USER_DICT)
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public AbstractTip freeze(@RequestParam Integer userId) {
        if (ToolUtil.isEmpty(userId)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        //???????????????????????????
        if (userId.equals(Const.ADMIN_ID)) {
            throw new BussinessException(BizExceptionEnum.CANT_FREEZE_ADMIN);
        }
        this.managerDao.setStatus(userId, ManagerStatus.FREEZED.getCode());
        return SUCCESS_TIP;
    }

    /**
     * ??????????????????
     */
    @RequestMapping("/unfreeze")
    @BussinessLog(value = "??????????????????", key = "userId", dict = Dict.USER_DICT)
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public AbstractTip unfreeze(@RequestParam Integer userId) {
        if (ToolUtil.isEmpty(userId)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        this.managerDao.setStatus(userId, ManagerStatus.OK.getCode());
        return SUCCESS_TIP;
    }

    /**
     * ????????????
     */
    @RequestMapping("/setRole")
    @BussinessLog(value = "????????????", key = "userId,roleIds", dict = Dict.USER_DICT)
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public AbstractTip setRole(@RequestParam("userId") Integer userId, @RequestParam("roleIds") String roleIds) {
        if (ToolUtil.isOneEmpty(userId, roleIds)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        //???????????????????????????
        if (userId.equals(Const.ADMIN_ID)) {
            throw new BussinessException(BizExceptionEnum.CANT_CHANGE_ADMIN);
        }
        this.managerDao.setRoles(userId, roleIds);
        return SUCCESS_TIP;
    }

    /**
     * ????????????(??????????????????webapp/static/img)
     */
    @RequestMapping(method = RequestMethod.POST, path = "/upload")
    @ResponseBody
    public String upload(@RequestPart("file") MultipartFile picture) {
        String pictureName = UUID.randomUUID().toString() + ".jpg";
        try {
            String fileSavePath = gunsProperties.getFileUploadPath();
            picture.transferTo(new File(fileSavePath + pictureName));
        } catch (Exception e) {
            throw new BussinessException(BizExceptionEnum.UPLOAD_ERROR);
        }
        return pictureName;
    }

    public static void main(String[] args) {
        JurisdictionStatus id= JurisdictionStatus.getMethod("1");
        System.out.println(id.getRoleId());
    }
}

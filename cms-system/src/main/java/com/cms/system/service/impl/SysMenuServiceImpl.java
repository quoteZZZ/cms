package com.cms.system.service.impl;

import com.cms.common.constant.Constants;
import com.cms.common.constant.UserConstants;
import com.cms.common.core.domain.TreeSelect;
import com.cms.common.core.domain.entity.SysMenu;
import com.cms.common.core.domain.entity.SysRole;
import com.cms.common.core.domain.entity.SysUser;
import com.cms.common.utils.SecurityUtils;
import com.cms.common.utils.StringUtils;
import com.cms.common.core.domain.vo.MetaVo;
import com.cms.common.core.domain.vo.RouterVo;
import com.cms.system.mapper.SysMenuMapper;
import com.cms.system.mapper.SysRoleMapper;
import com.cms.system.mapper.SysRoleMenuMapper;
import com.cms.system.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单业务层实现类：
 * 实现了系统菜单的业务逻辑处理，包括菜单查询、权限管理、路由构建和菜单的增删改查等操作。
 * @author quoteZZZ
 */
@Service
public class SysMenuServiceImpl implements ISysMenuService
{
    //权限字符串的通用格式，用于生成权限字符串，例如 "perms["system:user:list"]"
    public static final String PREMISSION_STRING = "perms[\"{0}\"]";

    @Autowired
    private SysMenuMapper menuMapper; // 菜单数据访问对象

    @Autowired
    private SysRoleMapper roleMapper; // 角色数据访问对象

    @Autowired
    private SysRoleMenuMapper roleMenuMapper; // 角色菜单关联数据访问对象

    /**
     * 根据用户查询系统菜单列表
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override //调用重载方法，并传入默认的空菜单对象
    public List<SysMenu> selectMenuList(Long userId)
    {
        return selectMenuList(new SysMenu(), userId);
    }

    /**
     * 查询系统菜单列表
     * @param menu 菜单信息（包含筛选条件）
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuList(SysMenu menu, Long userId) {
        List<SysMenu> menuList = null;
        if (SysUser.isAdmin(userId)) { // 判断是否为管理员用户
            menuList = menuMapper.selectMenuList(menu); // 管理员可查询所有菜单
        } else {
            menu.getParams().put("userId", userId); // 为普通用户添加查询条件
            menuList = menuMapper.selectMenuListByUserId(menu); // 查询与用户关联的菜单
        }
        return menuList;// 返回菜单列表
    }

    /**
     * 根据用户ID查询权限
     * @param userId 用户ID
     * @return 权限集合
     */
    @Override
    public Set<String> selectMenuPermsByUserId(Long userId) {
        // 查询用户权限字符串列表
        List<String> perms = menuMapper.selectMenuPermsByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (StringUtils.isNotEmpty(perm)) { // 非空校验
                // 按逗号分割权限并加入Set集合
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;// 返回权限集合
    }

    /**
     * 根据角色ID查询权限
     * @param roleId 角色ID
     * @return 权限集合
     */
    @Override
    public Set<String> selectMenuPermsByRoleId(Long roleId) {
        // 查询角色对应的权限字符串列表
        List<String> perms = menuMapper.selectMenuPermsByRoleId(roleId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (StringUtils.isNotEmpty(perm)) { // 非空校验
                // 按逗号分割权限并加入Set集合
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 根据用户ID构建菜单树
     * @param userId 用户ID
     * @return 菜单树列表
     */
    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        List<SysMenu> menus = null;
        if (SecurityUtils.isAdmin(userId)) { // 如果是管理员
            menus = menuMapper.selectMenuTreeAll(); // 获取所有菜单（权限）
        } else {
            menus = menuMapper.selectMenuTreeByUserId(userId); // 获取与用户关联的菜单
        }
        return getChildPerms(menus, 0); // 构建菜单树
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    @Override
    public List<Long> selectMenuListByRoleId(Long roleId) {
        // 从数据库中获取角色信息
        SysRole role = roleMapper.selectRoleById(roleId);
        // 根据角色ID和菜单选择策略查询菜单ID列表
        return menuMapper.selectMenuListByRoleId(roleId, role.isMenuCheckStrictly());
    }

    /**
     * 构建前端路由所需要的菜单（将菜单的树形结构转变成前端需要的路由格式返回前端）
     * @param menus 菜单列表（单的树形结构）
     * @return 路由列表（前端需要的路由格式）
     */
    @Override
    public List<RouterVo> buildMenus(List<SysMenu> menus) {
        // 初始化路由列表
        List<RouterVo> routers = new LinkedList<RouterVo>();
        // 遍历菜单列表，将每个菜单转换为前端路由
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            // 设置路由是否隐藏
            router.setHidden("1".equals(menu.getVisible()));
            // 设置路由名称
            router.setName(getRouteName(menu));
            // 设置路由路径
            router.setPath(getRouterPath(menu));
            // 设置组件路径
            router.setComponent(getComponent(menu));
            // 设置查询参数
            router.setQuery(menu.getQuery());
            // 设置路由的元信息（如标题、图标等）
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
            // 获取子菜单列表
            List<SysMenu> cMenus = menu.getChildren();
            // 如果菜单是目录类型且有子菜单，则递归构建子菜单
            if (StringUtils.isNotEmpty(cMenus) && UserConstants.TYPE_DIR.equals(menu.getMenuType())) {
                router.setAlwaysShow(true); // 设置总是显示子菜单
                router.setRedirect("noRedirect"); // 设置重定向路径（不重定向）
                router.setChildren(buildMenus(cMenus)); // 递归处理子菜单（构建子菜单列表的路由信息）
            }
            // 判断是否为主类目下组件菜单类型，如果是，创建子路由组成完整路径
            else if (isMenuFrame(menu)) {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                // 配置子路由的路径、组件、名称和元信息
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(getRouteName(menu.getRouteName(), menu.getPath()));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
                children.setQuery(menu.getQuery());
                // 添加子路由到子路由列表
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            // 如果菜单是HTTP开头的内链类型且为根目录，特殊处理内链（如果是，创建子路由，删除https://www.部分，改为内部链接路径）
            else if (menu.getParentId().intValue() == 0 && isInnerLink(menu)) {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
                router.setPath("/");
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                // 替换内链路径并设置相关信息
                String routerPath = innerLinkReplaceEach(menu.getPath());
                children.setPath(routerPath);
                children.setComponent(UserConstants.INNER_LINK);
                children.setName(getRouteName(menu.getRouteName(), routerPath));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                // 添加子路由到子路由列表
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            // 添加路由到列表中
            routers.add(router);
        }
        return routers;// 返回构建好的路由列表
    }

    /**
     * 构建前端所需要树结构
     * @param menus 菜单列表
     * @return 树结构列表
     */
    @Override
    public List<SysMenu> buildMenuTree(List<SysMenu> menus) {
        // 用于存储最终的树结构
        List<SysMenu> returnList = new ArrayList<SysMenu>();
        // 获取所有菜单ID的列表
        List<Long> tempList = menus.stream().map(SysMenu::getMenuId).collect(Collectors.toList());
        // 遍历菜单列表
        for (Iterator<SysMenu> iterator = menus.iterator(); iterator.hasNext();) {
            SysMenu menu = (SysMenu) iterator.next();// 获取当前菜单
            // 如果菜单是顶级节点（其父ID不存在于菜单ID列表中）
            if (!tempList.contains(menu.getParentId())) {
                // 递归查找子节点
                recursionFn(menus, menu);
                // 添加到树结构的返回列表中
                returnList.add(menu);
            }
        }
        // 如果未找到顶级节点，则直接返回原菜单列表
        if (returnList.isEmpty()) {
            returnList = menus;
        }
        return returnList;// 返回树结构列表
    }

    /**
     * 构建前端所需要下拉树结构
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildMenuTreeSelect(List<SysMenu> menus) {
        // 将菜单列表构建为树形结构
        List<SysMenu> menuTrees = buildMenuTree(menus);
        // 将树形结构转换为下拉树结构
        return menuTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据菜单ID查询信息
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    @Override
    public SysMenu selectMenuById(Long menuId) {
        return menuMapper.selectMenuById(menuId);
    }

    /**
     * 是否存在菜单子节点
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean hasChildByMenuId(Long menuId) {
        int result = menuMapper.hasChildByMenuId(menuId);
        return result > 0; // 如果有子节点，返回true
    }

    /**
     * 查询菜单使用数量
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean checkMenuExistRole(Long menuId) {
        int result = roleMenuMapper.checkMenuExistRole(menuId);
        return result > 0; // 如果被引用，返回true
    }

    /**
     * 新增保存菜单信息
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public int insertMenu(SysMenu menu) {
        return menuMapper.insertMenu(menu); // 调用Mapper方法插入菜单
    }

    /**
     * 修改保存菜单信息
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public int updateMenu(SysMenu menu) {
        return menuMapper.updateMenu(menu); // 调用Mapper方法更新菜单
    }

    /**
     * 删除菜单管理信息
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public int deleteMenuById(Long menuId) {
        return menuMapper.deleteMenuById(menuId); // 调用Mapper方法删除菜单
    }

    /**
     * 校验菜单名称是否唯一
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkMenuNameUnique(SysMenu menu) {
        // 获取菜单ID和菜单名称
        Long menuId = StringUtils.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        // 调用Mapper方法查询菜单名称
        SysMenu info = menuMapper.checkMenuNameUnique(menu.getMenuName(), menu.getParentId());
        //如果查询到信息，并且菜单ID不匹配，则返回不唯一
        if (StringUtils.isNotNull(info) && info.getMenuId().longValue() != menuId.longValue()) {
            return UserConstants.NOT_UNIQUE; // 返回不唯一
        }
        // 否则返回唯一
        return UserConstants.UNIQUE;
    }

    /**
     * 获取路由名称
     * @param menu 菜单信息
     * @return 路由名称
     */
    public String getRouteName(SysMenu menu)
    {
        // 非外链并且是一级目录（类型为目录）
        if (isMenuFrame(menu))
        {
            return StringUtils.EMPTY;//// 框架类型菜单返回空字符串
        }
        return getRouteName(menu.getRouteName(), menu.getPath());
    }

    /**
     * 获取路由名称，如没有配置路由名称则取路由地址
     * @param name 路由名称
     * @param path 路由地址
     * @return 路由名称（驼峰格式）
     */
    public String getRouteName(String name, String path)
    {
        String routerName = StringUtils.isNotEmpty(name) ? name : path;
        return StringUtils.capitalize(routerName);//// 返回驼峰格式名称
    }

    /**
     * 获取路由地址
     * @param menu 菜单信息
     * @return 路由地址
     */
    public String getRouterPath(SysMenu menu)
    {
        String routerPath = menu.getPath();
        // 内链打开外网方式
        if (menu.getParentId().intValue() != 0 && isInnerLink(menu))
        {
            routerPath = innerLinkReplaceEach(routerPath);
        }
        // 非外链并且是一级目录（类型为目录）
        if (0 == menu.getParentId().intValue() && UserConstants.TYPE_DIR.equals(menu.getMenuType())
                && UserConstants.NO_FRAME.equals(menu.getIsFrame()))
        {
            routerPath = "/" + menu.getPath();
        }
        // 非外链并且是一级目录（类型为菜单）
        else if (isMenuFrame(menu))
        {
            routerPath = "/";
        }
        return routerPath;
    }

    /**
     * 获取组件信息
     * @param menu 菜单信息
     * @return 组件信息
     */
    public String getComponent(SysMenu menu)
    {
        String component = UserConstants.LAYOUT;
        if (StringUtils.isNotEmpty(menu.getComponent()) && !isMenuFrame(menu))
        {
            component = menu.getComponent();
        }
        else if (StringUtils.isEmpty(menu.getComponent()) && menu.getParentId().intValue() != 0 && isInnerLink(menu))
        {
            component = UserConstants.INNER_LINK;
        }
        else if (StringUtils.isEmpty(menu.getComponent()) && isParentView(menu))
        {
            component = UserConstants.PARENT_VIEW;
        }
        return component;
    }

    /**
     * 是否为菜单内部跳转
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isMenuFrame(SysMenu menu)
    {
        return menu.getParentId().intValue() == 0 && UserConstants.TYPE_MENU.equals(menu.getMenuType())
                && menu.getIsFrame().equals(UserConstants.NO_FRAME);
    }

    /**
     * 是否为内链组件
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isInnerLink(SysMenu menu)
    {
        return menu.getIsFrame().equals(UserConstants.NO_FRAME) && StringUtils.ishttp(menu.getPath());
    }

    /**
     * 是否为parent_view组件
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isParentView(SysMenu menu)
    {
        return menu.getParentId().intValue() != 0 && UserConstants.TYPE_DIR.equals(menu.getMenuType());
    }

    /**
     * 根据父节点的ID获取所有子节点（构建树状菜单树的起点）
     * @param list 分类表（菜单列表）
     * @param parentId 传入的父节点ID
     * @return 子节点列表
     */
    public List<SysMenu> getChildPerms(List<SysMenu> list, int parentId) {
        // 定义一个存放子节点（子菜单/权限）的列表
        List<SysMenu> returnList = new ArrayList<SysMenu>();
        // 遍历分类表中的所有节点（菜单/权限），寻找符合父类ID的菜单
        for (Iterator<SysMenu> iterator = list.iterator(); iterator.hasNext();) {
            //获取当前迭代的菜单节点
            SysMenu t = (SysMenu) iterator.next();
            // 一、判断当前节点是否是指定父节点的直接子节点（如果当前菜单节点的父ID与传入的父节点ID一致）
            if (t.getParentId() == parentId) {
                // 二、递归获取该子节点的所有子节点
                recursionFn(list, t);
                // 将子节点添加到返回列表中
                returnList.add(t);
            }
        }
        return returnList; // 返回所有子节点
    }

    /**
     * 递归列表
     * @param list 分类表（菜单列表）
     * @param t 子节点
     */
    private void recursionFn(List<SysMenu> list, SysMenu t) {
        // 获取当前节点的直接子节点列表
        List<SysMenu> childList = getChildList(list, t);
        // 将子节点列表设置到当前节点
        t.setChildren(childList);
        // 遍历子节点列表
        for (SysMenu tChild : childList) {
            // 如果当前子节点还有子节点，则递归处理
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);// 递归处理子节点
            }
        }
    }

    /**
     * 得到子节点列表
     * @param list 分类表（菜单列表）
     * @param t 父节点
     * @return 子节点列表
     */
    private List<SysMenu> getChildList(List<SysMenu> list, SysMenu t) {
        // 定义一个存放子节点的列表
        List<SysMenu> tlist = new ArrayList<SysMenu>();
        // 遍历分类表（菜单列表）
        Iterator<SysMenu> it = list.iterator();
        while (it.hasNext()) {
            SysMenu n = (SysMenu) it.next();
            // 判断是否是当前节点的直接子节点
            if (n.getParentId().longValue() == t.getMenuId().longValue()) {
                tlist.add(n); //将子节点添加到子节点列表中
            }
        }
        return tlist; // 返回子节点列表
    }

    /**
     * 判断是否有子节点
     * @param list 分类表（菜单列表）
     * @param t 父节点
     * @return 是否有子节点
     */
    private boolean hasChild(List<SysMenu> list, SysMenu t) {
        // 判断当前节点是否存在子节点
        return getChildList(list, t).size() > 0;
    }

    /**
     * 内链域名特殊字符替换
     * @param path 内链域名路径
     * @return 替换后的内链域名
     */
    public String innerLinkReplaceEach(String path) {
        // 使用StringUtils替换内链路径中的特殊字符
        return StringUtils.replaceEach(path,
                new String[] { Constants.HTTP, Constants.HTTPS, Constants.WWW, ".", ":" },
                new String[] { "", "", "", "/", "/" });
    }

}

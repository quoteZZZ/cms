    /**
     * 查询推荐竞赛
     *
     * @param type 推荐类型（random、category、access、latest）
     * @param category 竞赛类别（仅在 type=category 时有效）
     * @param count 推荐数量
     * @return 推荐竞赛列表
     * @throws ServiceException 如果参数无效
     */
    public List<SysComp> recommendCompetitions(String type, Character category, int count);
import jdk.jfr.Description;

public enum TASKTYPE {
    // 查询机票,取消预定，修改预定，其他
    @Description("查询机票")
    QUERY_TICKET("查询机票"),
    @Description("取消预定")
    CANCEL_TICKET("取消预定"),
    @Description("修改预定")
    MODIFY_TICKET("修改预定"),
    @Description("OTHER")
    OTHER("其他");

    private final String name;

    TASKTYPE(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

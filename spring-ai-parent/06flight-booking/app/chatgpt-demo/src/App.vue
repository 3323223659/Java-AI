

<template>


  <el-row :gutter="20">
    <el-col  :span="span" :class="[span != '15' ? 'span1' : 'span1']">

      <el-table :data="tableData" stripe style="width: 100%">
        <el-table-column prop="bookingNumber" label="#"   />
        <el-table-column prop="name" label="Name" />
        <el-table-column prop="date" label="Date" />
        <el-table-column prop="from" label="From" />
        <el-table-column prop="to" label="To" />
        <el-table-column prop="bookingStatus" label="Status" >
          <template #default="scope">
            {{ scope.row.bookingStatus === "CONFIRMED" ? "✅" : "❌"}}
          </template>
        </el-table-column>
        <el-table-column prop="bookingClass" label="Booking class" />
        <el-table-column label="Operations" fixed="right" width="180" >
          <template #default="scope">
            <el-button size="small"
                       type="primary">
              更改预定
            </el-button>
            <el-button
                size="small"
                type="danger">
              退订
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="position: relative; width: 100%; height: 100%">
        <el-drawer
            class="drawerClass"
            style="position: absolute"
            :append-to-body="false"
            :modal="false"
            :show-close="false"
            :wrapperClosable="false"
            size="100%"
            :visible.sync="drawer"
            direction="ltr"
        >
          <el-card class="box-card" style="position: relative">
            <div>左侧内容</div>
          </el-card>
        </el-drawer>
        <div
            style="
              position: absolute;
              z-index: 999999999;
              cursor: pointer;
              top: 30%;
            "
            :class="[drawer ? 'imgright' : 'imgright1']"
            @click="clickImg"
        >
          <img
              v-show="!drawer"
              style="height: 40px; width: 25px"
              src="./assets/zhankai.png"
              alt=""
          />
          <img
              v-show="drawer"
              style="height: 40px; width: 25px"
              src="./assets/shousuo.png"
              alt=""
          />
        </div>
      </div>
    </el-col>

    <el-col  :span="span1" :class="[span1 != '8' ? 'span1' : 'span1']" style="background-color: aliceblue;">
      <div style="height: 500px;overflow: scroll">
        <el-timeline style="max-width: 100%">
          <el-timeline-item
              v-for="(activity, index) in activities"
              :key="index"
              :icon="activity.icon"
              :type="activity.type"
              :color="activity.color"
              :size="activity.size"
              :hollow="activity.hollow"
              :timestamp="activity.timestamp"
          >
            {{ activity.content }}
          </el-timeline-item>
        </el-timeline>
      </div>
      <div id="container">
        <div id="chat">
          <el-input
              v-model="msg"
              input-style="width: 100%;height:50px"
              :rows="2"
              type="text"
              placeholder="Please input"
              @keydown.enter="sendMsg();"
          />
          <el-button  @click="sendMsg()">发送</el-button>
        </div>
      </div>
    </el-col>
  </el-row>

</template>
<script lang="ts">
import { MoreFilled } from '@element-plus/icons-vue'
import {ref, onMounted} from "vue";
import axios from 'axios'//引入axios

export default {
  data() {
    return {
      span: 15,
      span1: 8,
      drawer: true,
    };
  },
  methods: {
    clickImg() {
      this.drawer = !this.drawer;
      if (this.drawer) {
        this.span = 15;
        this.span1 = 8;
      } else {
        this.span = 1;
        this.span1 = 23;
      }
    },
  },
  setup() {
    const activities = ref([
      {
        content: '⭐欢迎来到图灵航空✈！请问有什么可以帮您的?',
        timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
        color: '#0bbd87',
      },
    ]);
    const msg = ref('');
    const tableData = ref([]);
    let count = 2;
    let eventSource;

    const sendMsg = () => {
      if (eventSource) {
        eventSource.close();
      }

      activities.value.push(
          {
            content: `你:${msg.value}`,
            timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
            size: 'large',
            type: 'primary',
            icon: MoreFilled,
          },
      );

      activities.value.push(
          {
            content: 'waiting...',
            timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
            color: '#0bbd87',
          },
      );

      // sse: 服务端推送 Server-Sent Events
      eventSource = new EventSource(`http://localhost:8080/ai/generateStreamAsString?message=${msg.value}`);
      msg.value='';
      eventSource.onmessage = (event) => {
        activities.value[count].content += event.data;
      };
      eventSource.onopen = (event) => {
        activities.value[count].content = '';
      };
      eventSource.onerror = function (e) {
        count = count + 2;
        eventSource.close();
        getBookings();  // 每次对话完后刷新列表
      };
    };

    const getBookings = () => {
      axios.get('http://localhost:8080/booking/list')
          .then((response) => {
            debugger;
            tableData.value = response.data;
          })
          .catch((error) => {
            console.error(error);
          });
    };

    // Use onMounted to call getBookings when the component is mounted
    onMounted(() => {
      getBookings();
    });

    return {
      activities,
      msg,
      tableData,
      sendMsg,
      getBookings,
    };
  },
};
</script>
<style scoped>
  * {
    margin: 0;
    padding: 0;
  }
  #chat button{
  position: absolute;
  margin-left: -60px;
  margin-top: 19px;
  }
  .span1 {
    transition: all 0.2s;
  }
  .imgright {
    right: 0;
    background-color: #f5f5f5;
    transition: all 0.2s;
  }
  .imgright1 {
    left: 0;
    background-color: #fff;
    transition: all 0.2s;
  }
  .drawerClass ::v-deep .el-drawer__container .el-drawer .el-drawer__header {
    display: none;
  }
</style>

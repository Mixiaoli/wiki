<template>
  <a-layout class="doc-layout">
    <a-layout-content class="doc-layout-content">
      <h3 v-if="level1.length === 0" class="doc-empty">对不起，找不到相关文档！</h3>
      <a-row class="doc-row" gutter="24">
        <a-col :span="6" class="doc-tree-col">
          <div class="doc-tree-card" v-if="level1.length > 0">
            <div class="doc-tree-header">
              <h2>目录导航</h2>
              <p>选择章节查看对应内容</p>
            </div>
            <a-tree
                class="doc-tree"
                :tree-data="level1"
                @select="onSelect"
                :replaceFields="{title: 'name', key: 'id', value: 'id'}"
                :defaultExpandAll="true"
                :defaultSelectedKeys="defaultSelectedKeys"
            ><!--defaultSelectedKeys默认选中什么节点-->
            </a-tree>
          </div>
        </a-col>
        <a-col :span="18" class="doc-detail-col">
          <section class="doc-detail" v-if="doc && doc.id">
            <header class="doc-header">
              <div class="doc-header-main">
                <div class="doc-title-group">
                  <span class="doc-badge">章节</span>
                  <h1>{{ doc.name }}</h1>
                </div>
                <div class="doc-meta">
                  <span><component :is="'ReadOutlined'"/>阅读 {{ doc.viewCount }}</span>
                  <span><component :is="'LikeOutlined'"/>点赞 {{ doc.voteCount }}</span>
                </div>
              </div>
              <div class="doc-cover-wrapper" v-if="doc.cover">
                <img :src="buildFileUrl(doc.cover)" alt="文档封面" class="doc-cover"/>
              </div>
            </header>
            <a-divider class="doc-divider"/>
            <div class="doc-content">
              <div class="wangeditor" :innerHTML="html"></div>
            </div>
            <div class="vote-div">
              <a-button type="primary" shape="round" size="large" @click="vote">
                <template #icon>
                  <LikeOutlined/> &nbsp;点赞：{{ doc.voteCount }}
                </template>
              </a-button>
            </div>
          </section>
          <section v-else class="doc-placeholder">
            <h2>请选择左侧目录开始阅读</h2>
            <p>支持多层级章节结构，点击任意节点即可加载对应内容。</p>
          </section>
        </a-col>
      </a-row>
    </a-layout-content>
  </a-layout>
</template>


<script lang="ts">
import {defineComponent, onMounted, ref, createVNode} from 'vue';
import axios from 'axios';
import {message} from 'ant-design-vue';
import {Tool} from "@/util/tool";
import {useRoute} from "vue-router";

export default defineComponent({
  name: 'Doc',
  setup() {
    const route = useRoute();
    const docs = ref();
    const html = ref();
    const defaultSelectedKeys = ref();
    defaultSelectedKeys.value = []; //初始化
    // 当前选中的文档
    const doc = ref();
    doc.value = {};

    /**
     * 一级文档树，children属性就是二级文档
     * [{
     *   id: "",
     *   name: "",
     *   children: [{
     *     id: "",
     *     name: "",
     *   }]
     * }]
     */
    const level1 = ref(); // 一级文档树，children属性就是二级文档
    level1.value = [];

    /**
     * 内容查询
     **/
    const handleQueryContent = (id: number) => {
      axios.get("/doc/find-content/" + id).then((response) => {
        const data = response.data;
        if (data.success) {
          html.value = data.content;
        } else {
          message.error(data.message);
        }
      });
    };

    /**
     * 数据查询
     **/
    const handleQuery = () => {
      axios.get("/doc/all/" + route.query.ebookId).then((response) => {
        const data = response.data;
        if (data.success) {
          docs.value = data.content;

          level1.value = [];
          level1.value = Tool.array2Tree(docs.value, 0);

          if (Tool.isNotEmpty(level1)) {
            defaultSelectedKeys.value = [level1.value[0].id];
            handleQueryContent(level1.value[0].id);
            // 初始显示文档信息.
            doc.value = level1.value[0];
          }
        } else {
          message.error(data.message);
        }
      });
    };

    const onSelect = (selectedKeys: any, info: any) => {
      console.log('selected', selectedKeys, info);
      if (Tool.isNotEmpty(selectedKeys)) {
        // 选中某一节点时，加载该节点的文档信息
        doc.value = info.selectedNodes[0].props;
        // 加载内容
        handleQueryContent(selectedKeys[0]);
      }
    };

    // 点赞
    const vote = () => {
      axios.get('/doc/vote/' + doc.value.id).then((response) => {
        const data = response.data;
        if (data.success) {
          doc.value.voteCount++;
        } else {
          message.error(data.message);
        }
      });
    };

    onMounted(() => {
      handleQuery();
    });
    const buildFileUrl = Tool.buildFileUrl;
    return {
      level1,
      html,
      onSelect,
      defaultSelectedKeys,
      doc,
      vote,
      buildFileUrl
    }
  }
});
</script>
<style>
/* wangeditor默认样式, 参照: http://www.wangeditor.com/doc/pages/02-%E5%86%85%E5%AE%B9%E5%A4%84%E7%90%86/03-%E8%8E%B7%E5%8F%96ht
ml.html */
.doc-layout {
  min-height: calc(100vh - 120px);
  background: linear-gradient(145deg, #f5f7fb 0%, #eef2f8 100%);
  padding: 32px 0 48px;
}

.doc-layout-content {
  max-width: 1200px;
  margin: 0 auto;
  background: transparent;
}

.doc-empty {
  text-align: center;
  padding: 80px 0;
  color: #5f6c80;
}

.doc-row {
  align-items: stretch;
}

.doc-tree-col {
  display: flex;
}

.doc-tree-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 28px 24px;
  box-shadow: 0 16px 40px rgba(55, 94, 148, 0.12);
  border: 1px solid rgba(210, 220, 237, 0.8);
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.doc-tree-header h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2a44;
}

.doc-tree-header p {
  margin: 6px 0 0;
  color: #6b7a95;
  font-size: 13px;
}

.doc-tree {
  flex: 1;
  background: transparent;
}

.doc-tree :deep(.ant-tree-node-content-wrapper) {
  border-radius: 10px;
  padding: 6px 10px;
  transition: all 0.2s ease;
}

.doc-tree :deep(.ant-tree-node-content-wrapper:hover),
.doc-tree :deep(.ant-tree-node-selected) {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.doc-detail-col {
  display: flex;
}

.doc-detail {
  background: #ffffff;
  border-radius: 24px;
  padding: 32px 40px;
  box-shadow: 0 28px 50px rgba(43, 68, 118, 0.18);
  border: 1px solid rgba(218, 226, 240, 0.9);
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.doc-header {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  flex-wrap: wrap;
  gap: 20px;
}

.doc-header-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1 1 auto;
  min-width: 0;
}

.doc-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-title-group h1 {
  margin: 0;
  font-size: 28px;
  color: #1c2541;
}

.doc-badge {
  background: linear-gradient(120deg, #60a5fa, #3b82f6);
  color: #fff;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.doc-meta {
  display: flex;
  gap: 20px;
  color: #5f6c80;
  font-size: 14px;
}

.doc-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.doc-divider {
  margin: 0;
  height: 2px;
  background: linear-gradient(90deg, rgba(96, 165, 250, 0.5), transparent);
}

.doc-content {
  line-height: 1.8;
  color: #2f3a54;
  font-size: 15px;
}

.doc-content :deep(img) {
  max-width: 100%;
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.15);
}

.doc-content :deep(h1),
.doc-content :deep(h2),
.doc-content :deep(h3) {
  color: #1e293b;
}

.vote-div {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.vote-div :deep(.ant-btn) {
  min-width: 220px;
  box-shadow: 0 18px 30px rgba(59, 130, 246, 0.25);
}

.doc-placeholder {
  background: #ffffff;
  border-radius: 24px;
  padding: 64px 40px;
  text-align: center;
  color: #5f6c80;
  box-shadow: 0 24px 48px rgba(43, 68, 118, 0.14);
  border: 1px solid rgba(218, 226, 240, 0.9);
}

.doc-placeholder h2 {
  margin-bottom: 12px;
  color: #1f2a44;
}

/* wangeditor默认样式, 参照: http://www.wangeditor.com/doc/pages/02-%E5%86%85%E5%AE%B9%E5%A4%84%E7%90%86/03-%E8%8E%B7%E5%8F%96html.html */
/* table 样式 */
.wangeditor table {
  border-top: 1px solid #ccc;
  border-left: 1px solid #ccc;
}
.doc-cover-wrapper {
  flex-shrink: 0;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 16px 32px rgba(30, 64, 175, 0.18);
  border: 1px solid rgba(191, 219, 254, 0.7);
}

.doc-cover {
  display: block;
  width: 180px;
  height: 120px;
  object-fit: cover;
}

.wangeditor table td,
.wangeditor table th {
  border-bottom: 1px solid #ccc;
  border-right: 1px solid #ccc;
}

@media (max-width: 1024px) {
  .doc-row {
    flex-direction: column;
  }

  .doc-tree-col,
  .doc-detail-col {
    width: 100%;
  }

  .doc-detail,
  .doc-tree-card {
    border-radius: 18px;
  }
}

@media (max-width: 768px) {
  .doc-layout {
    padding: 24px 0 32px;
  }

  .doc-detail {
    padding: 24px;
  }

  .doc-meta {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
<template>
  <div class="home-page">
    <header class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <p class="hero-kicker">知识随手可查</p>
          <h1>欢迎来到Mi知识百科</h1>
          <p class="hero-description">
            汇聚团队沉淀的文档、教程与实践经验，像翻阅维基百科一样轻松探索，快速定位所需知识。
          </p>
          <div class="hero-actions">
            <a-button type="primary" size="large" shape="round" @click="scrollToContent">
              浏览分类
            </a-button>
            <a-button size="large" shape="round" @click="showWelcome">
              查看概览
            </a-button>
          </div>
        </div>
        <div class="hero-highlight">
          <div class="highlight-card">
            <component :is="'ReadOutlined'" class="highlight-icon"/>
            <div>
              <h3>结构化知识体系</h3>
              <p>清晰的层级分类，帮助你按主题逐步深入。</p>
            </div>
          </div>
          <div class="highlight-card">
            <component :is="'TeamOutlined'" class="highlight-icon"/>
            <div>
              <h3>IT数据</h3>
              <p>IT SOP&问题汇总</p>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main class="main-grid" id="content-anchor">
      <aside class="sidebar">
        <div class="sidebar-card">
          <h2>分类导航</h2>
          <p class="sidebar-description">按领域浏览文档，选择任意分类即可查看该主题下的文章。</p>
          <a-menu
              class="category-menu"
              mode="inline"
              @click="handleClick"
              :openKeys="openKeys"
          >
            <a-menu-item key="welcome">
              <component :is="'HomeOutlined'"/>
              <span>欢迎页</span>
            </a-menu-item>
            <a-sub-menu v-for="item in level1" :key="item.id" :disabled="true">
              <template #title>
                <span><component :is="'FolderOpenOutlined'"/>{{ item.name }}</span>
              </template>
              <a-menu-item v-for="child in item.children" :key="child.id">
                <component :is="'FileTextOutlined'"/>
                <span>{{ child.name }}</span>
              </a-menu-item>
            </a-sub-menu>
          </a-menu>
        </div>
      </aside>

      <section class="content-section">
        <transition name="fade" mode="out-in">
          <div key="welcome" v-if="isShowWelcome" class="welcome-panel">
            <the-welcome/>
          </div>
          <div key="articles" v-else class="article-panel">
            <header class="panel-header">
              <h2>精选文档</h2>
              <p>近期最受关注的文档合集，欢迎查阅与分享。</p>
            </header>
            <a-list
                class="article-list"
                item-layout="vertical"
                size="large"
                :grid="{ gutter: 24, column: 2 }"
                :data-source="ebooks"
            >
              <template #renderItem="{ item }">
                <a-list-item :key="item.name" class="article-card">
                  <template #actions>
                    <span>
                      <component :is="'FileOutlined'" class="article-action-icon"/>
                      {{ item.docCount }}
                    </span>
                    <span>
                      <component :is="'UserOutlined'" class="article-action-icon"/>
                      {{ item.viewCount }}
                    </span>
                    <span>
                      <component :is="'LikeOutlined'" class="article-action-icon"/>
                      {{ item.voteCount }}
                    </span>
                  </template>
                  <a-list-item-meta :description="item.description">
                    <template #title>
                      <router-link :to="'/doc?ebookId=' + item.id">
                        {{ item.name }}
                      </router-link>
                    </template>
                    <template #avatar>
                      <a-avatar
                          shape="square"
                          class="article-cover"
                          :src="buildFileUrl(item.cover)"
                      >
                        <template v-if="!item.cover">
                          <component :is="'BookOutlined'"/>
                        </template>
                      </a-avatar>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </transition>
      </section>
    </main>
  </div>
</template>

<script lang="ts">
import {defineComponent, onMounted, ref} from 'vue';
import axios from 'axios';
import {message} from 'ant-design-vue';
import {Tool} from "@/util/tool";
import TheWelcome from '@/components/the-welcome.vue';

// const listData: any = [];
// for (let i = 0; i < 23; i++) {
//   listData.push({
//     href: 'https://www.antdv.com/',
//     title: `ant design vue part ${i}`,
//     avatar: 'https://zos.alipayobjects.com/rmsportal/ODTLcjxAfvqbxHnVXCYX.png',
//     description:
//         'Ant Design, a design language for background applications, is refined by Ant UED Team.',
//     content:
//         'We supply a series of design principles, practical patterns and high quality design resources (Sketch and Axure), to
//help people create their product prototypes beautifully and efficiently.',
//   });
// }

export default defineComponent({//配置导入的组件
  name: 'Home',
  components: {
    TheWelcome
  },
  setup() {
    const ebooks = ref<any[]>([]);
    const openKeys = ref<string[]>([]);
    const level1 = ref<any[]>([]);
    let categorys: any;
    /**
     * 查询所有分类
     **/
    const handleQueryCategory = () => {
      axios.get("/category/all").then((response) => {
        const data = response.data;
        if (data.success) {
          categorys = data.content;
          console.log("原始数组：", categorys);

          // 加载完分类后，将侧边栏全部展开
          openKeys.value = [];
          for (let i = 0; i < categorys.length; i++) {
            openKeys.value.push(categorys[i].id)
          }

          level1.value = Tool.array2Tree(categorys, 0);
          console.log("树形结构：", level1.value);
        } else {
          message.error(data.message);
        }
      });
    };

    const isShowWelcome = ref(true);
    let categoryId2 = 0;

    const scrollToContent = () => {
      const element = document.getElementById('content-anchor');
      if (element) {
        element.scrollIntoView({behavior: 'smooth', block: 'start'});
      }
    };

    const handleQueryEbook = () => {
      axios.get("/ebook/list", {
        params: {
          page: 1,
          size: 1000,
          categoryId2: categoryId2
        }
      }).then((response) => {
        const data = response.data;
        ebooks.value = data.content.list;
        // ebooks1.books = data.content;
      });
    };

    const handleClick = (value: any) => {
      // console.log("menu click", value)
      // console.log("menu click", value)
      if (value.key === 'welcome') {
        isShowWelcome.value = true;
        scrollToContent();
      } else {
        categoryId2 = value.key;
        isShowWelcome.value = false;
        handleQueryEbook();
        scrollToContent();
      }
      // isShowWelcome.value = value.key === 'welcome';
    };

    const showWelcome = () => {
      isShowWelcome.value = true;
      scrollToContent();
    };

    onMounted(() => {
      handleQueryCategory();
      // handleQueryEbook();
    });
    const buildFileUrl = Tool.buildFileUrl;
    return {
      ebooks,
      pagination: {
        onChange: (page: any) => {
          console.log(page);
        },
        pageSize: 3,
      },
      handleClick,
      level1,
      isShowWelcome,
      openKeys,
      scrollToContent,
      showWelcome,
      buildFileUrl
    }
  }
});
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f5f6fa;
  color: #1f2933;
}

.hero {
  background: linear-gradient(135deg, rgba(228, 238, 250, 0.95) 0%, rgba(244, 247, 255, 0.95) 100%);
  border-bottom: 1px solid #dfe7f3;
  padding: 72px 0 96px;
  position: relative;
  overflow: hidden;
}

.hero::after {
  content: '';
  position: absolute;
  inset: -80px 20% auto auto;
  width: 420px;
  height: 420px;
  background: radial-gradient(circle at center, rgba(92, 142, 222, 0.35), transparent 65%);
  pointer-events: none;
}

.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
}

.hero-text {
  max-width: 620px;
  position: relative;
  z-index: 1;
}

.hero-kicker {
  letter-spacing: 0.28em;
  text-transform: uppercase;
  font-size: 12px;
  font-weight: 700;
  color: #3b82f6;
  margin-bottom: 12px;
}
.article-cover {
  width: 86px;
  height: 112px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 16px 32px rgba(30, 64, 175, 0.15);
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.12), rgba(37, 99, 235, 0.12));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  font-size: 24px;
}

.article-cover :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.hero h1 {
  font-size: 44px;
  font-weight: 800;
  line-height: 1.2;
  margin-bottom: 16px;
  color: #1b1f3b;
}

.hero-description {
  font-size: 18px;
  line-height: 1.7;
  color: #425466;
}

.hero-actions {
  margin-top: 32px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.hero-actions :deep(.ant-btn) {
  padding: 0 28px;
  height: 46px;
  font-size: 16px;
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.18);
}

.hero-actions :deep(.ant-btn-default) {
  background: rgba(255, 255, 255, 0.65);
  border-color: transparent;
  box-shadow: none;
}

.hero-highlight {
  display: grid;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.highlight-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 22px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 18px;
  box-shadow: 0 20px 35px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(209, 224, 255, 0.6);
}

.highlight-card h3 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2a56;
}

.highlight-card p {
  margin: 0;
  color: #506079;
  font-size: 14px;
}

.highlight-icon {
  font-size: 30px;
  color: #3b82f6;
}

.main-grid {
  max-width: 1200px;
  margin: -72px auto 40px;
  padding: 0 32px 48px;
  display: grid;
  gap: 32px;
  grid-template-columns: 320px 1fr;
  position: relative;
  z-index: 2;
}

.sidebar-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 28px 24px;
  box-shadow: 0 16px 32px rgba(43, 68, 117, 0.08);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.sidebar-card h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #243056;
}

.sidebar-description {
  margin-top: 12px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #5a6b85;
  line-height: 1.6;
}

.category-menu {
  background: transparent;
  border: none;
}
category-menu :deep(.ant-menu-item),
.category-menu :deep(.ant-menu-submenu-title) {
  height: 44px;
  display: flex;
  align-items: center;
  border-radius: 12px;
  margin: 4px 0;
  font-weight: 500;
  color: #425466;
}

.category-menu :deep(.ant-menu-item:hover),
.category-menu :deep(.ant-menu-submenu-title:hover) {
  background: rgba(59, 130, 246, 0.08);
  color: #2563eb;
}

.category-menu :deep(.ant-menu-item-selected) {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.18), rgba(79, 70, 229, 0.16));
  color: #1d4ed8;
  font-weight: 600;
}

.category-menu :deep(.ant-menu-submenu-title .ant-menu-title-content span),
.category-menu :deep(.ant-menu-item .ant-menu-title-content span) {
  margin-left: 8px;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.welcome-panel,
.article-panel {
  background: #ffffff;
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(226, 232, 240, 0.7);
  min-height: 420px;
}

.panel-header {
  margin-bottom: 24px;
}

.panel-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.panel-header p {
  margin-top: 8px;
  margin-bottom: 0;
  color: #64748b;
  font-size: 15px;
}

.article-list :deep(.ant-list-items) {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.article-card {
  border-radius: 18px;
  padding: 20px 24px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 1px solid rgba(226, 232, 240, 0.7);
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 35px rgba(15, 23, 42, 0.12);
}

.article-card :deep(.ant-list-item-meta-title a) {
  color: #1f2937;
  font-weight: 600;
}

.article-card :deep(.ant-list-item-meta-description) {
  color: #64748b;
}

.article-action-icon {
  color: #6366f1;
  margin-right: 6px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 1024px) {
  .main-grid {
    grid-template-columns: 1fr;
    margin-top: -48px;
  }

  .hero-inner {
    flex-direction: column;
    text-align: center;
  }

  .hero-actions {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .hero {
    padding: 56px 0 72px;
  }

  .hero h1 {
    font-size: 34px;
  }

  .main-grid {
    padding: 0 20px 32px;
  }

  .sidebar-card {
    padding: 22px 20px;
  }

  .welcome-panel,
  .article-panel {
    padding: 24px;
  }
}

@media (max-width: 576px) {
  .hero-description {
    font-size: 16px;
  }

  .article-list :deep(.ant-list-items) {
    grid-template-columns: 1fr;
  }
}
</style>
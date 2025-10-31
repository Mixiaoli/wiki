<template>
  <a-layout>
    <a-layout-content
        :style="{ background: '#fff', padding: '24px', margin: 0, minHeight: '280px' }"
    >
      <a-row :gutter="24">
        <a-col :span="8">
          <p>
            <a-form layout="inline" :model="param">
              <a-form-item>
                <a-input v-model:value="param.name" placeholder="名称">
                </a-input>
              </a-form-item>
              <a-form-item>
                <a-button type="primary" @click="handleQuery()">
                  查询
                </a-button>
              </a-form-item>
              <a-form-item>
                <a-button type="primary" @click="add()">
                  新增
                </a-button>
              </a-form-item>
            </a-form>
          </p>
          <!--列,key id,数据doc,分页,等待框,分页执行方法-->      <!--pagintaion是否要分页 这里是否-->
          <a-table
              v-if="level1.length > 0"
              :columns="columns"
              :row-key="record => record.id"
              :data-source="level1"
              :loading="loading"
              :pagination="false"
              size="small"
              :defaultExpandAllRows="true"
          >
            <template #name="{text, record }">
              {{record.sort}} {{text}} <!--text电子书名-->
            </template>
            <template v-slot:action="{ text, record }">
              <a-space size="small">
                <!--一整行的数据-->
                <a-button type="primary" @click="edit(record)" size="small">
                  编辑
                </a-button>
                <a-popconfirm
                    title="删除后不可恢复，确认删除?"
                    ok-text="是"
                    cancel-text="否"
                    @confirm="handleDelete(record.id)"
                >
                  <a-button type="primary" size="small">
                    删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </a-table>
        </a-col>
        <a-col :span="16">
          <p>
            <a-form layout="inline" :model="param">
              <a-form-item>
                <a-button type="primary" @click="handleSave()">
                  保存
                </a-button>
              </a-form-item>
            </a-form>
          </p>
          <!--弹出表单-->
          <a-form :model="doc" layout="vertical">
            <a-form-item>
              <a-input v-model:value="doc.name" placeholder="名称"/>
            </a-form-item>
            <a-form-item label="父文档">
              <a-tree-select
                  v-model:value="doc.parent"
                  style="width: 100%"
                  :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                  :tree-data="treeSelectData"
                  placeholder="请选择父文档"
                  tree-default-expand-all
                  :replaceFields="{title: 'name', key: 'id', value: 'id'}"
              >
              </a-tree-select>
            </a-form-item>
            <a-form-item label="顺序">
              <a-input v-model:value="doc.sort" placeholder="顺序"/>
            </a-form-item>
            <a-form-item label="封面">
              <div class="cover-upload">
                <img v-if="doc.cover" :src="buildFileUrl(doc.cover)" alt="封面预览" class="cover-preview"/>
                <a-space align="start">
                  <a-upload
                      :show-upload-list="false"
                      :custom-request="handleCoverUpload"
                      :disabled="coverUploading"
                      accept="image/*"
                  >
                    <a-button type="primary" :loading="coverUploading">
                      上传封面
                    </a-button>
                  </a-upload>
                  <a-button v-if="doc.cover" @click="clearCover" :disabled="coverUploading">
                    移除
                  </a-button>
                </a-space>
              </div>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handlePreviewContent()">
                <EyeOutlined/>
                内容预览
              </a-button>
            </a-form-item>
            <a-form-item> <!--这里就是导入wangditor富文本的代码-->
              <div id="content">
              </div>
            </a-form-item>
          </a-form>
        </a-col>
      </a-row>

      <a-drawer width="900" placement="right" :closable="false" :visible="drawerVisible" @close="onDrawerClose">
        <div class="wangeditor" :innerHTML="previewHtml"></div>
      </a-drawer>

    </a-layout-content>
  </a-layout>
  <!--  <a-modal
        title="文档表单"
        v-model:visible="modalVisible"
        :confirm-loading="modalLoading"
        @ok="handleModalOk"
    ></a-modal>-->
</template>

<script lang="ts">
import {createVNode, defineComponent, onMounted, ref} from 'vue';//写上onMounted VUE3.0 setup集成了 导入ref 做响应式数据
import axios from 'axios';
import {message, Modal} from "ant-design-vue";//ant ui 消息组件
import {Tool} from "@/util/tool";
import {useRoute} from "vue-router";
import ExclamationCircleOutlined from "@ant-design/icons-vue/ExclamationCircleOutlined";
import E from 'wangeditor';

interface RcCustomRequestOptions {
  file: File | Blob | { originFileObj?: File | Blob };
  onProgress?: (event: { percent: number }) => void;
  onSuccess?: (body: any, file?: File, xhr?: XMLHttpRequest) => void;
  onError?: (error: Error) => void;
  [key: string]: any;
}

export default defineComponent({
  name: 'AdminDoc',
  setup() {
    const route = useRoute();//导入路由
    console.log("路由：", route);
    console.log("route.path：", route.path);
    console.log("route.query：", route.query);
    console.log("route.param：", route.params);
    console.log("route.fullPath：", route.fullPath);
    console.log("route.name：", route.name);
    console.log("route.meta：", route.meta);
    const param = ref();
    param.value = {};
    const docs = ref();//响应式数据 获取的书籍实时反馈到页面上
    const loading = ref(false);
    const coverUploading = ref(false);

    // 因为树选择组件的属性状态，会随当前编辑的节点而变化，所以单独声明一个响应式变量
    const treeSelectData = ref();
    treeSelectData.value = [];

    const columns = [
      {
        title: '名称',
        dataIndex: 'name',
        slots: {customRender: 'name'}
      },
      {
        title: 'Action',
        key: 'action',
        slots: {customRender: 'action'}
      }


    ];
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
    const level1 = ref();
    level1.value = [];
    /**
     * 数据查询
     **/
    const handleQuery = () => {
      loading.value = true;
      axios.get("/doc/all/" + route.query.ebookId).then((response) => {
        loading.value = false;
        const data = response.data;
        if (data.success) {
          docs.value = data.content;
          console.log("原始数组：", docs.value);

          level1.value = [];
          level1.value = Tool.array2Tree(docs.value, 0);//调用tool.ts做树形结构
          console.log("树形结构：", level1);

          // 父文档下拉框初始化，相当于点击新增
          treeSelectData.value = Tool.copy(level1.value) || [];
          // 为选择树添加一个"无"
          treeSelectData.value.unshift({id: 0, name: '无'});
        } else {
          message.error(data.message);
        }
      });
    };

    // -------- 表单 ---------
    /**
     * 数组，[100, 101]对应：前端开发 / Vue
     */
        // 因为树选择组件的属性状态，会随当前编辑的节点而变化，所以单独声明一个响应式变量
    const doc = ref();//表单
    doc.value = {
      ebookId: route.query.ebookId,
      cover: ''
    };
    const modalVisible = ref(false);//显示弹窗
    const modalLoading = ref(false);//时间加载
    const buildFileUrl = (url: string) => {
      if (!url) {
        return url;
      }
      if (/^https?:\/\//i.test(url)) {
        return url;
      }
      const base = (axios.defaults.baseURL || process.env.VUE_APP_SERVER || '') as string;
      if (!base) {
        return url;
      }
      const normalizedBase = base.replace(/\/$/, '');
      const normalizedUrl = url.replace(/^\//, '');
      return `${normalizedBase}/${normalizedUrl}`;
    };

    const uploadFiles = async (files: File[]) => {
      if (!files || files.length === 0) {
        return [];
      }
      try {
        const formData = new FormData();
        files.forEach(file => formData.append('file', file));
        const response = await axios.post('/file/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        const data = response.data;
        if (data.success && data.content) {
          const urls = Array.isArray(data.content) ? data.content : [data.content];
          return urls as string[];
        }
        throw new Error(data.message || '图片上传失败');
      } catch (error: any) {
        if (error?.response?.data?.message) {
          throw new Error(error.response.data.message);
        }
        throw new Error(error?.message || '图片上传失败');
      }
    };

    const editor = new E('#content');
    editor.config.zIndex = 0;

    editor.config.pasteFilterStyle = false; // 允许保留粘贴内容的内联样式
    editor.config.pasteIgnoreImg = false; // 允许保留粘贴内容中的图片
    const insertClipboardHtml = async (pasteEvent: ClipboardEvent): Promise<boolean> => {
      const clipboardData = pasteEvent.clipboardData || (window as Window & { clipboardData?: DataTransfer }).clipboardData;
      if (!clipboardData) {
        return false;
      }
      const items = clipboardData.items ? Array.from(clipboardData.items) : [];
      const imageFiles: File[] = items
          .filter((item) => item.kind === 'file' && item.type && item.type.indexOf('image') === 0)
          .map((item) => item.getAsFile())
          .filter((file): file is File => !!file);

      if (imageFiles.length > 0) {
        pasteEvent.preventDefault();
        try {
          const urls = await uploadFiles(imageFiles);
          urls.forEach((url: string) => {
            const imageHtml = `<img src="${buildFileUrl(url)}" style="max-width: 100%;" />`;
            editor.cmd.do('insertHTML', imageHtml);
          });
        } catch (error: any) {
          message.error(error?.message || '图片上传失败');
        }
        return true;
      }
      const html = clipboardData.getData('text/html');
      if (!html) {
        return false;
      }
      pasteEvent.preventDefault();
      const cleanedHtml = html
          .replace(/<!--[\s\S]*?-->/g, '')
          .replace(/<meta[^>]*?>/gi, '')
          .replace(/<!DOCTYPE[^>]*?>/gi, '')
          .replace(/<\/?(html|body)[^>]*?>/gi, '');
      editor.cmd.do('insertHTML', cleanedHtml);
      return true;
    };

    //用于图片上传
    editor.config.customUploadImg = async (resultFiles: File[], insertImgFn: (url: string) => void) => {
      try {
        const urls = await uploadFiles(resultFiles);
        urls.forEach((url: string) => {
          insertImgFn(buildFileUrl(url));
        });
      } catch (error: any) {
        message.error(error?.message || '图片上传失败');
      }
    };

    const handleCoverUpload = async (options: RcCustomRequestOptions) => {
      const { file, onError, onSuccess } = options;
      if (!file) {
        const err = new Error('请选择文件');
        onError && onError(err);
        return;
      }
      coverUploading.value = true;
      try {
        const urls = await uploadFiles([file as File]);
        if (urls.length === 0) {
          throw new Error('未返回封面地址');
        }
        const uploadedUrl = urls[0];
        if (!doc.value) {
          doc.value = { ebookId: route.query.ebookId, cover: '' } as any;
        }
        doc.value.cover = uploadedUrl;
        onSuccess && onSuccess(uploadedUrl);
        message.success('封面上传成功');
      } catch (error: any) {
        const err = error instanceof Error ? error : new Error(error?.message || '封面上传失败');
        message.error(err.message || '封面上传失败');
        onError && onError(err);
      } finally {
        coverUploading.value = false;
      }
    };

    const clearCover = () => {
      if (!doc.value) {
        return;
      }
      doc.value.cover = '';
    };
    const handleSave = () => {
      modalLoading.value = true;
      doc.value.content = editor.txt.html();
      console.log(doc.value.content + "!!!!1");
      //下面那个doc就是 doc=ref()绑定到表单的doc
      axios.post("/doc/save", doc.value).then((response) => {
        modalLoading.value = false;
        const data = response.data;//data = commonResp 返回提交的业务是成功的话success=true
        if (data.success) {
          modalVisible.value = false;
          message.success("保存成功！");
          //重新加载列表
          handleQuery();
        } else {
          message.error(data.message);
        }
      });
    };

    /**
     * 将某节点及其子孙节点全部置为disabled
     */
    const setDisable = (treeSelectData: any, id: any) => {
      // console.log(treeSelectData, id);
      // 遍历数组，即遍历某一层节点
      for (let i = 0; i < treeSelectData.length; i++) {
        const node = treeSelectData[i];
        if (node.id === id) {
          // 如果当前节点就是目标节点
          console.log("disabled", node);
          // 将目标节点设置为disabled
          node.disabled = true;

          // 遍历所有子节点，将所有子节点全部都加上disabled
          const children = node.children;
          if (Tool.isNotEmpty(children)) {
            for (let j = 0; j < children.length; j++) {
              setDisable(children, children[j].id)
            }
          }
        } else {
          // 如果当前节点不是目标节点，则到其子节点再找找看。
          const children = node.children;
          if (Tool.isNotEmpty(children)) {
            setDisable(children, id);
          }
        }
      }
    };
    /**
     * 查找整根树枝 查找后来删除做操作
     */
    const deleteIds: Array<string> = [];
    const deleteNames: Array<string> = [];

    const getDeleteIds = (treeSelectData: any, id: any) => {
      // console.log(treeSelectData, id);
      // 遍历数组，即遍历某一层节点
      for (let i = 0; i < treeSelectData.length; i++) {
        const node = treeSelectData[i];
        if (node.id === id) {
          // 如果当前节点就是目标节点
          console.log("delete", node);
          // 将目标ID放入结果集ids
          // node.disabled = true;
          deleteIds.push(id);
          deleteNames.push(node.name);

          // 遍历所有子节点
          const children = node.children;
          if (Tool.isNotEmpty(children)) {
            for (let j = 0; j < children.length; j++) {
              getDeleteIds(children, children[j].id)
            }
          }
        } else {
          // 如果当前节点不是目标节点，则到其子节点再找找看。
          const children = node.children;
          if (Tool.isNotEmpty(children)) {
            getDeleteIds(children, id);
          }
        }
      }
    };
    /**
     * 内容查询
     **/
    const handleQueryContent = () => {
      axios.get("/doc/find-content/" + doc.value.id).then((response) => {
        loading.value = false;
        const data = response.data;
        if (data.success) {
          editor.txt.html(data.content);
        } else {
          message.error(data.message);
        }
      });
    };
    /**
     * 编辑
     */
    const edit = (record: any) => {
      //清空富文本框
      editor.txt.html("");
      modalVisible.value = true;
      doc.value = Tool.copy(record);
      doc.value.cover = doc.value.cover || '';
      handleQueryContent();
      // 不能选择当前节点及其所有子孙节点，作为父节点，会使树断开
      treeSelectData.value = Tool.copy(level1.value);
      setDisable(treeSelectData.value, record.id);

      // 为选择树添加一个"无"
      treeSelectData.value.unshift({id: 0, name: '无'});
    };

    /**
     * 新增
     */
    const add = () => {
      modalVisible.value = true;
      editor.txt.html("");
      doc.value = {
        ebookId: route.query.ebookId,
        cover: ''
      };

      treeSelectData.value = Tool.copy(level1.value);

      // 为选择树添加一个"无"
      treeSelectData.value.unshift({id: 0, name: '无'});
    };
    /**
     * 删除
     */
    const handleDelete = (id: number) => {
      // console.log(level1, level1.value, id)
      // 清空数组，否则多次删除时，数组会一直增加
      deleteIds.length = 0;
      deleteNames.length = 0;
      getDeleteIds(level1.value, id);
      Modal.confirm({
        title: '重要提醒',
        icon: createVNode(ExclamationCircleOutlined),
        content: '将删除：【' + deleteNames.join("，") + "】删除后不可恢复，确认删除？",
        onOk() {
          // console.log(ids)
          axios.delete("/doc/delete/" + deleteIds.join(",")).then((response) => {
            const data = response.data; // data = commonResp
            if (data.success) {
              // 重新加载列表
              handleQuery();
            } else {
              message.error(data.message);
            }
          });
        },
      });
    };
    // ----------------富文本预览--------------
    const drawerVisible = ref(false);
    const previewHtml = ref();
    const handlePreviewContent = () => {
      const html = editor.txt.html();
      previewHtml.value = html;
      drawerVisible.value = true;
    };
    const onDrawerClose = () => {
      drawerVisible.value = false;
    };

    onMounted(() => {
      handleQuery();
      editor.create();//等页面渲染好了 再去create
      //文档样式保留
      const originalPasteEvents = [...editor.txt.eventHooks.pasteEvents];
      editor.txt.eventHooks.pasteEvents.length = 0;
      editor.txt.eventHooks.pasteEvents.push((pasteEvent: ClipboardEvent) => {
      insertClipboardHtml(pasteEvent).then((handled) => {
          if (handled) {
            return;
          }
          originalPasteEvents.forEach((fn) => fn(pasteEvent));
        });
      });
    });
    return {
      param,
      level1,
      //docs,//表格
      columns,
      loading,
      handleQuery,

      edit,//表单
      add,
      doc,//doc返回到html
      modalVisible,
      modalLoading,
      handleSave,
      handleDelete,
      treeSelectData,

      drawerVisible,
      previewHtml,
      handlePreviewContent,
      onDrawerClose,
      buildFileUrl,
      handleCoverUpload,
      clearCover,
      coverUploading,
    }
  }
});
</script>

<!-- #scoped表示当前组件才有用 -->
<style scoped>
.img-wh {
  width: 50px;
  height: 50px;
  line-height: 50px;
  border-radius: 8%;
  margin: 5px 0;
}
cover-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cover-preview {
  width: 160px;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
}
</style>
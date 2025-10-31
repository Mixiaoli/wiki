<template>
  <div class="ip-planner">
    <a-page-header
        class="planner-header"
        title="IP 规划工具"
        sub-title="快速完成 IPv4 / IPv6 掩码换算与子网规划"
    />
    <a-layout class="tool-layout">
      <a-layout-sider width="220" class="tool-sider">
        <div class="tool-sider__title">工具分类</div>
        <a-menu
            mode="inline"
            :selectedKeys="[activeKey]"
            @click="handleMenuClick"
        >
          <a-menu-item key="ipv4">
            IPv4 规划
          </a-menu-item>
          <a-menu-item key="ipv6">
            IPv6 规划
          </a-menu-item>
          <a-menu-item key="network">
            网络测试
          </a-menu-item>
          <a-menu-item key="dhcp">
            DHCP 检测
          </a-menu-item>
        </a-menu>
      </a-layout-sider>
      <a-layout-content class="tool-content">
        <transition name="fade" mode="out-in">
          <section v-if="activeKey === 'ipv4'" key="ipv4" class="tool-panel">
            <a-card title="IPv4 子网规划" :bordered="false">
              <p class="panel-description">
                输入网络前缀或子网掩码，系统会自动同步并给出主机数量、可用主机数以及拆分后的子网数量。
              </p>
              <a-form layout="vertical">
                <a-row :gutter="16">
                  <a-col :span="12">
                    <a-form-item label="网络前缀长度 (CIDR)">
                      <a-input-number
                          v-model:value="ipv4State.prefixLength"
                          :min="0"
                          :max="32"
                          :precision="0"
                          style="width: 100%"
                          @change="onIpv4PrefixChange"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="子网掩码">
                      <a-input
                          v-model:value="ipv4State.subnetMask"
                          placeholder="例如：255.255.255.0"
                          @blur="onIpv4MaskBlur"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>
                <a-row :gutter="16">
                  <a-col :span="12">
                    <a-form-item label="拆分到的前缀长度">
                      <a-input-number
                          v-model:value="ipv4State.splitPrefix"
                          :min="ipv4State.prefixLength"
                          :max="32"
                          :precision="0"
                          style="width: 100%"
                          @change="onIpv4SplitChange"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="通配符掩码 (Wildcard)">
                      <a-input v-model:value="ipv4Summary.wildcardMask" disabled />
                    </a-form-item>
                  </a-col>
                </a-row>
              </a-form>

              <div class="result-grid">
                <div class="result-item">
                  <h4>主机位数</h4>
                  <p>{{ ipv4Summary.hostBits }} 位</p>
                </div>
                <div class="result-item">
                  <h4>地址数量</h4>
                  <p>{{ ipv4Summary.totalHosts }}</p>
                </div>
                <div class="result-item">
                  <h4>可用主机数</h4>
                  <p>{{ ipv4Summary.usableHosts }}</p>
                </div>
                <div class="result-item">
                  <h4>可划分子网数</h4>
                  <p>{{ ipv4Summary.subnetCount }}</p>
                  <small v-if="ipv4Summary.subnetHint">{{ ipv4Summary.subnetHint }}</small>
                </div>
              </div>

              <a-divider orientation="left">IPv4 地址分析</a-divider>
              <p class="panel-description">
                输入任意 IPv4 地址（支持 CIDR 形式），系统会即时计算对应的网络范围、广播地址、上一/下一子网等细节信息。
              </p>
              <a-form layout="vertical" class="analysis-form">
                <a-row :gutter="16">
                  <a-col :span="16">
                    <a-form-item label="IPv4 地址 (支持 CIDR)">
                      <a-input
                          v-model:value="ipv4AddressState.input"
                          placeholder="例如：10.132.28.1/25"
                          allow-clear
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="8">
                    <a-form-item label="前缀长度 (备用)">
                      <a-input-number
                          v-model:value="ipv4AddressState.prefixLength"
                          :min="0"
                          :max="32"
                          :precision="0"
                          style="width: 100%"
                          @change="onIpv4AddressPrefixChange"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>
              </a-form>
              <a-alert
                  v-if="!ipv4Analysis.ready"
                  :type="ipv4Analysis.status"
                  :message="ipv4Analysis.message"
                  show-icon
                  class="analysis-alert"
              />
              <a-descriptions
                  v-else
                  bordered
                  :column="1"
                  size="small"
                  class="analysis-table"
              >
                <a-descriptions-item label="压缩地址">
                  {{ ipv4Analysis.normalized }}
                </a-descriptions-item>
                <a-descriptions-item label="二进制地址">
                  {{ ipv4Analysis.binary }}
                </a-descriptions-item>
                <a-descriptions-item label="子网掩码">
                  {{ ipv4Analysis.subnetMask }}
                </a-descriptions-item>
                <a-descriptions-item label="通配符掩码">
                  {{ ipv4Analysis.wildcardMask }}
                </a-descriptions-item>
                <a-descriptions-item label="网络地址">
                  {{ ipv4Analysis.network }}
                </a-descriptions-item>
                <a-descriptions-item label="广播地址">
                  {{ ipv4Analysis.broadcast }}
                </a-descriptions-item>
                <a-descriptions-item label="可用地址范围">
                  {{ ipv4Analysis.usableRange }}
                </a-descriptions-item>
                <a-descriptions-item label="地址数量">
                  {{ ipv4Analysis.totalHosts }}
                </a-descriptions-item>
                <a-descriptions-item label="可用主机数">
                  {{ ipv4Analysis.usableHosts }}
                </a-descriptions-item>
                <a-descriptions-item label="上一子网">
                  {{ ipv4Analysis.previousNetwork }}
                </a-descriptions-item>
                <a-descriptions-item label="下一子网">
                  {{ ipv4Analysis.nextNetwork }}
                </a-descriptions-item>
                <a-descriptions-item label="地址类别">
                  {{ ipv4Analysis.addressClass }}
                </a-descriptions-item>
                <a-descriptions-item label="地址属性">
                  {{ ipv4Analysis.addressType }}
                </a-descriptions-item>
                <a-descriptions-item label="IPv4 映射 IPv6">
                  {{ ipv4Analysis.ipv4MappedIpv6 }}
                </a-descriptions-item>
                <a-descriptions-item label="6to4 地址">
                  {{ ipv4Analysis.sixToFour }}
                </a-descriptions-item>
                <a-descriptions-item label="反向 DNS 域">
                  {{ ipv4Analysis.reverseDns }}
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </section>
          <section v-else-if="activeKey === 'ipv6'" key="ipv6" class="tool-panel">
            <a-card title="IPv6 子网规划" :bordered="false">
              <p class="panel-description">
                IPv6 以前缀长度定义网络。请输入所需前缀，可查看对应掩码与可分配数量，并支持拆分子网估算。
              </p>
              <a-form layout="vertical">
                <a-row :gutter="16">
                  <a-col :span="12">
                    <a-form-item label="网络前缀长度 (CIDR)">
                      <a-input-number
                          v-model:value="ipv6State.prefixLength"
                          :min="0"
                          :max="128"
                          :precision="0"
                          style="width: 100%"
                          @change="onIpv6PrefixChange"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="拆分到的前缀长度">
                      <a-input-number
                          v-model:value="ipv6State.splitPrefix"
                          :min="ipv6State.prefixLength"
                          :max="128"
                          :precision="0"
                          style="width: 100%"
                          @change="onIpv6SplitChange"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>
                <a-row>
                  <a-col :span="24">
                    <a-form-item label="子网掩码表示">
                      <a-input v-model:value="ipv6Summary.subnetMask" disabled />
                    </a-form-item>
                  </a-col>
                </a-row>
              </a-form>

              <div class="result-grid">
                <div class="result-item">
                  <h4>主机位数</h4>
                  <p>{{ ipv6Summary.hostBits }} 位</p>
                </div>
                <div class="result-item">
                  <h4>地址数量</h4>
                  <p>{{ ipv6Summary.totalAddresses }}</p>
                </div>
                <div class="result-item">
                  <h4>可划分子网数</h4>
                  <p>{{ ipv6Summary.subnetCount }}</p>
                  <small v-if="ipv6Summary.subnetHint">{{ ipv6Summary.subnetHint }}</small>
                </div>
              </div>
            </a-card>
          </section>
          <section v-else-if="activeKey === 'network'" key="network" class="tool-panel">
            <a-card title="网络连通性测试" :bordered="false">
              <p class="panel-description">
                输入域名或 IP 地址，系统将通过公共 DNS over HTTPS 服务尝试解析记录，测量响应时间，并展示查询结果。
              </p>
              <a-form layout="vertical" class="network-form">
                <a-row :gutter="16">
                  <a-col :span="16">
                    <a-form-item label="目标 (域名 / IP)">
                      <a-input
                          v-model:value="networkTestState.target"
                          placeholder="例如：example.com 或 8.8.8.8"
                          allow-clear
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="8" class="network-form__actions">
                    <a-form-item label="操作">
                      <a-space>
                        <a-button type="primary" :loading="networkTestState.loading" @click="runNetworkTest">
                          开始测试
                        </a-button>
                        <a-button @click="resetNetworkTest" :disabled="networkTestState.loading && !networkTestState.error">
                          重置
                        </a-button>
                      </a-space>
                    </a-form-item>
                  </a-col>
                </a-row>
              </a-form>
              <a-alert
                  v-if="networkTestState.error"
                  type="error"
                  :message="networkTestState.error"
                  show-icon
                  class="analysis-alert"
              />
              <template v-else-if="networkTestState.result">
                <a-alert
                    :type="networkTestState.result.success ? 'success' : 'warning'"
                    :message="networkTestState.result.statusText"
                    :description="`总耗时：${networkTestState.result.latency} ms`"
                    show-icon
                    class="analysis-alert"
                />
                <a-descriptions bordered :column="1" size="small" class="analysis-table">
                  <a-descriptions-item label="测试目标">
                    {{ networkTestState.result.target }}
                  </a-descriptions-item>
                  <a-descriptions-item label="测试时间">
                    {{ networkTestState.result.timestamp }}
                  </a-descriptions-item>
                  <a-descriptions-item v-if="networkTestState.result.queryNote" label="备注">
                    {{ networkTestState.result.queryNote }}
                  </a-descriptions-item>
                </a-descriptions>
                <a-divider orientation="left">解析详情</a-divider>
                <div
                    v-for="detail in networkTestState.result.details"
                    :key="detail.label"
                    class="network-detail"
                >
                  <div class="network-detail__header">
                    <h4>{{ detail.label }}</h4>
                    <span :class="['network-detail__status', detail.success ? 'is-success' : 'is-warning']">
                      {{ detail.statusText }} · {{ detail.latency }} ms
                    </span>
                  </div>
                  <div v-if="detail.answers.length" class="network-detail__body">
                    <ul>
                      <li v-for="(answer, index) in detail.answers" :key="`${detail.label}-${index}`">
                        <span class="network-answer__record">{{ answer.data }}</span>
                        <span class="network-answer__meta">TTL {{ answer.ttl }}s · 类型 {{ answer.typeName }}</span>
                      </li>
                    </ul>
                  </div>
                  <a-empty v-else description="暂无记录" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
                </div>
              </template>
            </a-card>
          </section>
          <section v-else key="dhcp" class="tool-panel">
            <a-card title="DHCP 设备检测" :bordered="false">
              <p class="panel-description">
                浏览器将尝试访问常见网关地址以判断是否存在疑似 DHCP / 小型路由器设备。由于网络与安全限制，检测结果仅供参考。
              </p>
              <a-alert
                  type="info"
                  message="如需精准的 DHCP 嗅探，请在受信任的网络环境中使用专业安全工具；本检测只能识别到网络层可达的常见网关。"
                  show-icon
                  class="analysis-alert"
              />
              <a-space class="dhcp-actions">
                <a-button type="primary" :loading="dhcpDetectionState.running" @click="runDhcpDetection">
                  开始检测
                </a-button>
                <a-button @click="resetDhcpDetection" :disabled="dhcpDetectionState.running">
                  重置
                </a-button>
              </a-space>
              <a-alert
                  v-if="dhcpDetectionState.error"
                  type="error"
                  :message="dhcpDetectionState.error"
                  show-icon
                  class="analysis-alert"
              />
              <template v-else>
                <a-alert
                    v-if="dhcpDetectionState.results.length"
                    :type="dhcpPositiveCount > 0 ? 'warning' : 'success'"
                    :message="dhcpDetectionSummary"
                    :description="dhcpDetectionState.lastRun ? `检测时间：${dhcpDetectionState.lastRun}` : ''"
                    show-icon
                    class="analysis-alert"
                />
                <a-empty
                    v-else
                    description="尚未执行检测"
                    :image="Empty.PRESENTED_IMAGE_SIMPLE"
                    class="dhcp-empty"
                />
              </template>
              <div v-if="dhcpDetectionState.results.length" class="dhcp-result-list">
                <div
                    v-for="result in dhcpDetectionState.results"
                    :key="result.id"
                    class="dhcp-result"
                >
                  <div class="dhcp-result__header">
                    <div class="dhcp-result__title">
                      <h4>{{ result.label }}</h4>
                      <span class="dhcp-result__target">{{ result.protocol.toUpperCase() }}://{{ result.host }}</span>
                    </div>
                    <span :class="['dhcp-result__status', `is-${result.status}`]">
                      {{ dhcpStatusText[result.status] }} · {{ result.duration }} ms
                    </span>
                  </div>
                  <p class="dhcp-result__note">{{ result.note }}</p>
                </div>
              </div>
            </a-card>
          </section>
        </transition>
      </a-layout-content>
    </a-layout>
  </div>
</template>

<script lang="ts">
import {defineComponent, reactive, ref, computed, watch} from 'vue';
import {message, Empty} from 'ant-design-vue';

interface Ipv4State {
  prefixLength: number;
  subnetMask: string;
  splitPrefix: number;
}

interface Ipv6State {
  prefixLength: number;
  splitPrefix: number;
}

interface Ipv4AddressState {
  input: string;
  prefixLength: number;
}

interface NetworkAnswer {
  data: string;
  ttl: number;
  type: number;
  typeName: string;
}

interface NetworkTestDetail {
  label: string;
  latency: number;
  success: boolean;
  statusText: string;
  answers: NetworkAnswer[];
}

interface NetworkTestResultState {
  target: string;
  latency: number;
  timestamp: string;
  success: boolean;
  statusText: string;
  queryNote: string;
  details: NetworkTestDetail[];
}

interface NetworkToolState {
  target: string;
  loading: boolean;
  error: string;
  result: NetworkTestResultState | null;
}

type DhcpProbeStatus = 'reachable' | 'timeout' | 'blocked' | 'error';

interface DhcpProbeResult {
  id: string;
  host: string;
  label: string;
  protocol: 'http' | 'https';
  status: DhcpProbeStatus;
  duration: number;
  note: string;
}

interface DhcpDetectionState {
  running: boolean;
  error: string;
  lastRun: string | null;
  results: DhcpProbeResult[];
}

type Ipv4AnalysisResult =
    | {
  ready: false;
  status: 'info' | 'warning';
  message: string;
}
    | {
  ready: true;
  normalized: string;
  binary: string;
  subnetMask: string;
  wildcardMask: string;
  network: string;
  broadcast: string;
  usableRange: string;
  totalHosts: string;
  usableHosts: string;
  previousNetwork: string;
  nextNetwork: string;
  addressClass: string;
  addressType: string;
  ipv4MappedIpv6: string;
  sixToFour: string;
  reverseDns: string;
};

const clamp = (value: number | string | null | undefined, min: number, max: number): number => {
  const numeric = typeof value === 'string' ? Number(value) : value;
  if (numeric === null || numeric === undefined || Number.isNaN(numeric)) {
    return min;
  }
  return Math.min(Math.max(Math.round(numeric), min), max);
};

const formatPowerOfTwo = (bits: number): string => {
  if (bits <= 52) {
    return Number(Math.pow(2, bits)).toLocaleString();
  }
  return `2^${bits}`;
};

const ipv4Pattern = /^(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}$/;

const sanitizeNetworkTarget = (value: string): string => {
  const trimmed = value.trim();
  if (!trimmed) {
    return '';
  }
  return trimmed
      .replace(/^https?:\/\//i, '')
      .replace(/[#?].*$/, '')
      .replace(/\/.*$/, '')
      .trim();
};

const expandIpv6 = (value: string): string[] | null => {
  const input = value.trim();
  if (!input) {
    return null;
  }
  const lower = input.toLowerCase();
  const doubleColonSegments = lower.split('::');
  if (doubleColonSegments.length > 2) {
    return null;
  }
  const left = doubleColonSegments[0]
      ? doubleColonSegments[0].split(':').filter((segment) => segment !== '')
      : [];
  const right = doubleColonSegments.length === 2 && doubleColonSegments[1]
      ? doubleColonSegments[1].split(':').filter((segment) => segment !== '')
      : [];
  if (doubleColonSegments.length === 1 && left.length !== 8) {
    return null;
  }
  if (doubleColonSegments.length === 2 && left.length + right.length > 7) {
    return null;
  }
  const fillCount = doubleColonSegments.length === 2 ? 8 - left.length - right.length : 0;
  const fill = new Array(fillCount).fill('0');
  const combined = [...left, ...fill, ...right];
  if (combined.length !== 8) {
    return null;
  }
  const normalized: string[] = [];
  for (const part of combined) {
    if (!/^[0-9a-f]{1,4}$/.test(part)) {
      return null;
    }
    normalized.push(part.padStart(4, '0'));
  }
  return normalized;
};

type ParsedNetworkTarget =
    | { kind: 'ipv4'; address: string }
    | { kind: 'ipv6'; groups: string[] }
    | { kind: 'domain'; name: string };

const parseNetworkTarget = (input: string): ParsedNetworkTarget | null => {
  if (ipv4Pattern.test(input)) {
    return { kind: 'ipv4', address: input };
  }
  const ipv6 = expandIpv6(input);
  if (ipv6) {
    return { kind: 'ipv6', groups: ipv6 };
  }
  if (/^[a-z0-9-]+(\.[a-z0-9-]+)*$/i.test(input)) {
    return { kind: 'domain', name: input.toLowerCase() };
  }
  return null;
};

interface NetworkQueryTask {
  label: string;
  name: string;
  type: number;
  typeName: string;
}

const createQueryTasks = (parsed: ParsedNetworkTarget): { tasks: NetworkQueryTask[]; note: string } => {
  if (parsed.kind === 'domain') {
    return {
      note: '已对域名执行 A / AAAA 记录查询 (Google DoH)',
      tasks: [
        { label: 'A 记录查询', name: parsed.name, type: 1, typeName: 'A' },
        { label: 'AAAA 记录查询', name: parsed.name, type: 28, typeName: 'AAAA' },
      ],
    };
  }
  if (parsed.kind === 'ipv4') {
    return {
      note: '已执行 IPv4 PTR 反向解析 (Google DoH)',
      tasks: [
        {
          label: 'PTR 反向解析',
          name: `${parsed.address.split('.').reverse().join('.')}.in-addr.arpa`,
          type: 12,
          typeName: 'PTR',
        },
      ],
    };
  }
  const reversed = parsed.groups.join('').split('').reverse().join('.');
  return {
    note: '已执行 IPv6 PTR 反向解析 (Google DoH)',
    tasks: [
      {
        label: 'PTR 反向解析',
        name: `${reversed}.ip6.arpa`,
        type: 12,
        typeName: 'PTR',
      },
    ],
  };
};

const dnsStatusText = (status: number): string => {
  const statusMap: Record<number, string> = {
    0: '成功 (NOERROR)',
    1: '格式错误 (FORMERR)',
    2: '服务器故障 (SERVFAIL)',
    3: '域名不存在 (NXDOMAIN)',
    4: '不支持的查询 (NOTIMP)',
    5: '请求被拒绝 (REFUSED)',
    6: 'YXDOMAIN',
    7: 'YXRRSET',
    8: 'NXRRSET',
    9: 'NOTAUTH',
    10: 'NOTZONE',
  };
  return statusMap[status] ?? `未知状态 (${status})`;
};

const getRecordTypeName = (type: number): string => {
  const recordTypeMap: Record<number, string> = {
    1: 'A',
    2: 'NS',
    5: 'CNAME',
    6: 'SOA',
    12: 'PTR',
    13: 'HINFO',
    15: 'MX',
    16: 'TXT',
    28: 'AAAA',
    33: 'SRV',
    35: 'NAPTR',
  };
  return recordTypeMap[type] ?? `类型 ${type}`;
};

const computeWildcardMask = (mask: string): string => {
  const parts = mask.split('.');
  if (parts.length !== 4) {
    return '';
  }
  return parts
      .map((part) => {
        const num = Number(part);
        if (Number.isNaN(num)) {
          return '0';
        }
        return String(255 - num);
      })
      .join('.');
};

const prefixToIpv4Mask = (prefix: number): string => {
  const segments: number[] = [];
  let remaining = prefix;
  for (let i = 0; i < 4; i += 1) {
    const bits = Math.min(Math.max(remaining, 0), 8);
    let value = 0;
    for (let j = 0; j < bits; j += 1) {
      value |= 1 << (7 - j);
    }
    segments.push(value);
    remaining -= bits;
  }
  return segments.join('.');
};

const maskToPrefix = (mask: string): number | null => {
  const parts = mask.split('.');
  if (parts.length !== 4) {
    return null;
  }
  let binary = '';
  for (const part of parts) {
    if (part.trim() === '') {
      return null;
    }
    const num = Number(part);
    if (!Number.isInteger(num) || num < 0 || num > 255) {
      return null;
    }
    binary += num.toString(2).padStart(8, '0');
  }
  if (!/^1*0*$/.test(binary)) {
    return null;
  }
  return binary.indexOf('0') === -1 ? binary.length : binary.indexOf('0');
};

const prefixToIpv6Mask = (prefix: number): string => {
  const segments: string[] = [];
  let remaining = prefix;
  for (let i = 0; i < 8; i += 1) {
    const bits = Math.min(Math.max(remaining, 0), 16);
    let value = 0;
    for (let j = 0; j < bits; j += 1) {
      value |= 1 << (15 - j);
    }
    segments.push(value.toString(16).padStart(4, '0'));
    remaining -= bits;
  }
  return segments.join(':');
};

const parseIpv4Input = (input: string): { octets: number[]; prefix: number | null } | null => {
  const trimmed = input.trim();
  if (!trimmed) {
    return null;
  }
  let address = trimmed;
  let prefix: number | null = null;
  const slashIndex = trimmed.indexOf('/');
  if (slashIndex >= 0) {
    address = trimmed.slice(0, slashIndex);
    const suffix = trimmed.slice(slashIndex + 1);
    if (suffix !== '') {
      const parsedPrefix = Number(suffix);
      if (!Number.isInteger(parsedPrefix) || parsedPrefix < 0 || parsedPrefix > 32) {
        return null;
      }
      prefix = parsedPrefix;
    }
  }
  const parts = address.split('.');
  if (parts.length !== 4) {
    return null;
  }
  const octets: number[] = [];
  for (const part of parts) {
    if (part.trim() === '') {
      return null;
    }
    const octet = Number(part);
    if (!Number.isInteger(octet) || octet < 0 || octet > 255) {
      return null;
    }
    octets.push(octet);
  }
  return { octets, prefix };
};

const ipv4OctetsToNumber = (octets: number[]): number =>
    octets.reduce((acc, octet) => acc * 256 + octet, 0);

const numberToIpv4 = (value: number): string => {
  const octets: number[] = [];
  for (let shift = 24; shift >= 0; shift -= 8) {
    octets.push((value >>> shift) & 0xff);
  }
  return octets.join('.');
};

const formatIpv4Binary = (octets: number[]): string => octets.map((octet) => octet.toString(2).padStart(8, '0')).join('.');

const describeIpv4Class = (firstOctet: number): string => {
  if (firstOctet <= 127) {
    return 'A 类地址';
  }
  if (firstOctet <= 191) {
    return 'B 类地址';
  }
  if (firstOctet <= 223) {
    return 'C 类地址';
  }
  if (firstOctet <= 239) {
    return 'D 类地址 (组播)';
  }
  return 'E 类地址 (保留)';
};

const describeIpv4Type = (octets: number[]): string => {
  const [o1, o2] = octets;
  if (o1 === 10) {
    return '私有地址 (10.0.0.0/8)';
  }
  if (o1 === 172 && o2 >= 16 && o2 <= 31) {
    return '私有地址 (172.16.0.0/12)';
  }
  if (o1 === 192 && o2 === 168) {
    return '私有地址 (192.168.0.0/16)';
  }
  if (o1 === 169 && o2 === 254) {
    return '链路本地地址 (169.254.0.0/16)';
  }
  if (o1 === 127) {
    return '环回地址 (127.0.0.0/8)';
  }
  if (o1 >= 224 && o1 <= 239) {
    return '多播地址 (224.0.0.0/4)';
  }
  if (o1 >= 240) {
    return '保留地址 (240.0.0.0/4)';
  }
  return '公有地址';
};

const toIpv4MappedIpv6 = (octets: number[]): string => `::ffff:${octets.join('.')}`;

const toSixToFour = (octets: number[]): string => {
  const high = ((octets[0] << 8) | octets[1]).toString(16).padStart(4, '0');
  const low = ((octets[2] << 8) | octets[3]).toString(16).padStart(4, '0');
  return `2002:${high}:${low}::`;
};

export default defineComponent({
  name: 'IpPlanner',
  setup() {
    const activeKey = ref<'ipv4' | 'ipv6' | 'network' | 'dhcp'>('ipv4');

    const networkTestState = reactive<NetworkToolState>({
      target: '',
      loading: false,
      error: '',
      result: null,
    });

    const dhcpDetectionState = reactive<DhcpDetectionState>({
      running: false,
      error: '',
      lastRun: null,
      results: [],
    });

    const ipv4State = reactive<Ipv4State>({
      prefixLength: 24,
      subnetMask: prefixToIpv4Mask(24),
      splitPrefix: 24,
    });

    const ipv4AddressState = reactive<Ipv4AddressState>({
      input: '10.132.28.1/25',
      prefixLength: 25,
    });

    const ipv6State = reactive<Ipv6State>({
      prefixLength: 64,
      splitPrefix: 64,
    });

    const ipv4Summary = computed(() => {
      const hostBits = 32 - ipv4State.prefixLength;
      const totalHosts = formatPowerOfTwo(hostBits);
      const usableHosts = ipv4State.prefixLength >= 31
          ? '不适用'
          : Number(Math.max(Math.pow(2, hostBits) - 2, 0)).toLocaleString();
      const subnetDiff = ipv4State.splitPrefix - ipv4State.prefixLength;
      const subnetCount = subnetDiff < 0 ? '0' : formatPowerOfTwo(subnetDiff);
      let subnetHint = '';
      if (subnetDiff < 0) {
        subnetHint = '拆分前缀需大于或等于原前缀';
      } else if (subnetDiff === 0) {
        subnetHint = '无需拆分时子网数为 1';
      }
      return {
        hostBits,
        totalHosts,
        usableHosts,
        subnetCount,
        subnetHint,
        wildcardMask: computeWildcardMask(ipv4State.subnetMask),
      };
    });

    const ipv6Summary = computed(() => {
      const hostBits = 128 - ipv6State.prefixLength;
      const totalAddresses = formatPowerOfTwo(hostBits);
      const subnetDiff = ipv6State.splitPrefix - ipv6State.prefixLength;
      const subnetCount = subnetDiff < 0 ? '0' : formatPowerOfTwo(subnetDiff);
      let subnetHint = '';
      if (subnetDiff < 0) {
        subnetHint = '拆分前缀需大于或等于原前缀';
      } else if (subnetDiff === 0) {
        subnetHint = '无需拆分时子网数为 1';
      }
      return {
        hostBits,
        totalAddresses,
        subnetCount,
        subnetHint,
        subnetMask: prefixToIpv6Mask(ipv6State.prefixLength),
      };
    });

    const dhcpCandidates = [
      { host: '192.168.0.1', label: '家庭路由常见网关 (192.168.0.0/24)' },
      { host: '192.168.1.1', label: '家庭路由常见网关 (192.168.1.0/24)' },
      { host: '192.168.31.1', label: '智能路由常见网关 (小米/Redmi)' },
      { host: '192.168.10.1', label: '企业路由常见网关 (192.168.10.0/24)' },
      { host: '192.168.123.1', label: '小型路由默认网关 (腾达/水星)' },
      { host: '10.0.0.1', label: '10.0.0.0/8 常见网关 (运营商网关)' },
      { host: '10.1.1.1', label: '10.1.1.0/24 常见网关 (企业/酒店)' },
      { host: '172.16.0.1', label: '172.16.0.0/12 常见网关 (企业)' },
    ] as const;

    const dhcpStatusText: Record<DhcpProbeStatus, string> = {
      reachable: '可能存在 DHCP 设备',
      timeout: '目标无响应',
      blocked: '请求被浏览器阻止',
      error: '检测失败',
    };

    const dhcpDetectionSummary = computed(() => {
      if (!dhcpDetectionState.results.length) {
        return '尚未执行检测';
      }
      const reachable = dhcpDetectionState.results.filter((item) => item.status === 'reachable');
      if (reachable.length > 0) {
        return `发现 ${reachable.length} 个可达的常见网关，请确认是否为预期设备。`;
      }
      return '未检测到可达的常见网关，如需进一步排查请使用专业网络扫描工具。';
    });

    const dhcpPositiveCount = computed(() => dhcpDetectionState.results.filter((item) => item.status === 'reachable').length);

    const probeDhcpGateway = async (
        protocol: 'http' | 'https',
        host: string,
        label: string,
    ): Promise<DhcpProbeResult> => {
      const controller = new AbortController();
      const timeout = window.setTimeout(() => controller.abort(), 3500);
      const start = performance.now();
      const isHttpsPage = window.location.protocol === 'https:';
      const url = `${protocol}://${host}/?dhcp-detect=${Date.now()}`;
      try {
        await fetch(url, {
          mode: 'no-cors',
          cache: 'no-store',
          signal: controller.signal,
        });
        const duration = Math.round(performance.now() - start);
        window.clearTimeout(timeout);
        return {
          id: `${host}-${protocol}`,
          host,
          label,
          protocol,
          status: 'reachable',
          duration,
          note: '网络请求成功，疑似存在可响应的路由/DHCP 服务。',
        };
      } catch (error) {
        const duration = Math.round(performance.now() - start);
        window.clearTimeout(timeout);
        if (error instanceof DOMException && error.name === 'AbortError') {
          return {
            id: `${host}-${protocol}`,
            host,
            label,
            protocol,
            status: 'timeout',
            duration,
            note: '在 3.5 秒内未收到响应，目标可能不存在或拒绝连接。',
          };
        }
        if (isHttpsPage && protocol === 'http') {
          return {
            id: `${host}-${protocol}`,
            host,
            label,
            protocol,
            status: 'blocked',
            duration,
            note: 'HTTPS 页面访问 HTTP 资源被浏览器拦截，如需检测请尝试使用 HTTP 访问站点或浏览器扩展。',
          };
        }
        return {
          id: `${host}-${protocol}`,
          host,
          label,
          protocol,
          status: 'error',
          duration,
          note: `请求失败：${error instanceof Error ? error.message : String(error)}`,
        };
      }
    };

    const runDhcpDetection = async () => {
      if (dhcpDetectionState.running) {
        return;
      }
      dhcpDetectionState.running = true;
      dhcpDetectionState.error = '';
      dhcpDetectionState.results = [];
      try {
        const protocols: Array<'http' | 'https'> = window.location.protocol === 'https:'
            ? ['https', 'http']
            : ['http', 'https'];
        const results: DhcpProbeResult[] = [];
        for (const candidate of dhcpCandidates) {
          for (const protocol of protocols) {
            const result = await probeDhcpGateway(protocol, candidate.host, candidate.label);
            results.push(result);
            if (result.status === 'reachable') {
              break;
            }
          }
        }
        dhcpDetectionState.results = results;
        dhcpDetectionState.lastRun = new Date().toLocaleString();
      } catch (error) {
        dhcpDetectionState.error = `检测过程中发生异常：${error instanceof Error ? error.message : String(error)}`;
      } finally {
        dhcpDetectionState.running = false;
      }
    };

    const resetDhcpDetection = () => {
      if (dhcpDetectionState.running) {
        return;
      }
      dhcpDetectionState.error = '';
      dhcpDetectionState.lastRun = null;
      dhcpDetectionState.results = [];
    };

    watch(
        () => ipv4AddressState.input,
        (value) => {
          const parsed = parseIpv4Input(value);
          if (parsed && parsed.prefix !== null) {
            ipv4AddressState.prefixLength = clamp(parsed.prefix, 0, 32);
          }
        },
    );

    watch(
        () => networkTestState.target,
        () => {
          if (networkTestState.error) {
            networkTestState.error = '';
          }
        },
    );

    const ipv4Analysis = computed<Ipv4AnalysisResult>(() => {
      const trimmed = ipv4AddressState.input.trim();
      if (!trimmed) {
        return {
          ready: false,
          status: 'info' as const,
          message: '请输入 IPv4 地址，例如 10.132.28.1/25',
        };
      }
      const parsed = parseIpv4Input(trimmed);
      if (!parsed) {
        return {
          ready: false,
          status: 'warning' as const,
          message: '地址无效，请检查格式（例如：10.132.28.1 或 10.132.28.1/25）',
        };
      }
      const octets = parsed.octets;
      const prefixLength = clamp(parsed.prefix ?? ipv4AddressState.prefixLength, 0, 32);
      const ipInt = ipv4OctetsToNumber(octets);
      const hostBits = 32 - prefixLength;
      const blockSize = 2 ** hostBits;
      const network = Math.floor(ipInt / blockSize) * blockSize;
      const broadcast = network + blockSize - 1;
      const previousNetwork = network >= blockSize ? network - blockSize : null;
      const nextNetwork = network + blockSize <= 0xffffffff ? network + blockSize : null;
      const firstHost = prefixLength >= 31 ? null : network + 1;
      const lastHost = prefixLength >= 31 ? null : broadcast - 1;
      const totalHosts = blockSize.toLocaleString();
      const usableHosts = prefixLength >= 31 ? '不适用' : (blockSize - 2).toLocaleString();
      const wildcardMask = computeWildcardMask(prefixToIpv4Mask(prefixLength));
      let usableRange = '不适用';
      if (firstHost !== null && lastHost !== null) {
        usableRange = `${numberToIpv4(firstHost)} - ${numberToIpv4(lastHost)}`;
      }
      return {
        ready: true,
        normalized: `${octets.join('.')}/${prefixLength}`,
        binary: formatIpv4Binary(octets),
        subnetMask: prefixToIpv4Mask(prefixLength),
        wildcardMask: wildcardMask || '—',
        network: `${numberToIpv4(network)}/${prefixLength}`,
        broadcast: numberToIpv4(broadcast),
        usableRange,
        totalHosts,
        usableHosts,
        previousNetwork: previousNetwork === null ? '已是第一个子网' : `${numberToIpv4(previousNetwork)}/${prefixLength}`,
        nextNetwork: nextNetwork === null ? '已是最后一个子网' : `${numberToIpv4(nextNetwork)}/${prefixLength}`,
        addressClass: describeIpv4Class(octets[0]),
        addressType: describeIpv4Type(octets),
        ipv4MappedIpv6: toIpv4MappedIpv6(octets),
        sixToFour: toSixToFour(octets),
        reverseDns: `${octets.slice().reverse().join('.')}.in-addr.arpa`,
      };
    });

    const onIpv4PrefixChange = (value: number) => {
      const normalized = clamp(value, 0, 32);
      if (normalized !== ipv4State.prefixLength) {
        ipv4State.prefixLength = normalized;
      }
      ipv4State.subnetMask = prefixToIpv4Mask(ipv4State.prefixLength);
      if (ipv4State.splitPrefix < ipv4State.prefixLength) {
        ipv4State.splitPrefix = ipv4State.prefixLength;
      }
    };

    const onIpv4SplitChange = (value: number) => {
      const normalized = clamp(value, ipv4State.prefixLength, 32);
      if (normalized !== ipv4State.splitPrefix) {
        ipv4State.splitPrefix = normalized;
      }
    };

    const onIpv4MaskBlur = () => {
      const prefix = maskToPrefix(ipv4State.subnetMask);
      if (prefix === null) {
        message.error('请输入有效的 IPv4 子网掩码');
        ipv4State.subnetMask = prefixToIpv4Mask(ipv4State.prefixLength);
        return;
      }
      ipv4State.prefixLength = prefix;
      if (ipv4State.splitPrefix < prefix) {
        ipv4State.splitPrefix = prefix;
      }
      ipv4State.subnetMask = prefixToIpv4Mask(prefix);
    };

    const onIpv6PrefixChange = (value: number) => {
      const normalized = clamp(value, 0, 128);
      if (normalized !== ipv6State.prefixLength) {
        ipv6State.prefixLength = normalized;
      }
      if (ipv6State.splitPrefix < ipv6State.prefixLength) {
        ipv6State.splitPrefix = ipv6State.prefixLength;
      }
    };

    const onIpv6SplitChange = (value: number) => {
      const normalized = clamp(value, ipv6State.prefixLength, 128);
      if (normalized !== ipv6State.splitPrefix) {
        ipv6State.splitPrefix = normalized;
      }
    };

    const runNetworkTest = async () => {
      const sanitized = sanitizeNetworkTarget(networkTestState.target);
      if (!sanitized) {
        message.warning('请输入要测试的目标');
        return;
      }
      const parsed = parseNetworkTarget(sanitized);
      if (!parsed) {
        networkTestState.error = '目标格式无效，请输入正确的域名或 IP 地址';
        networkTestState.result = null;
        message.error(networkTestState.error);
        return;
      }
      const normalizedTarget = parsed.kind === 'domain' ? parsed.name : sanitized;
      const { tasks, note } = createQueryTasks(parsed);
      networkTestState.loading = true;
      networkTestState.error = '';
      networkTestState.result = null;
      networkTestState.target = normalizedTarget;
      const overallStart = performance.now();
      const details: NetworkTestDetail[] = [];
      let fallbackMessage = '';
      try {
        for (const task of tasks) {
          const detailStart = performance.now();
          try {
            const response = await fetch(
                `https://dns.google/resolve?name=${encodeURIComponent(task.name)}&type=${task.type}`,
                {
                  headers: { accept: 'application/dns-json' },
                  cache: 'no-store',
                },
            );
            const detailLatency = Math.round(performance.now() - detailStart);
            if (!response.ok) {
              const text = `${task.typeName} 查询失败：HTTP ${response.status}`;
              if (!fallbackMessage) {
                fallbackMessage = text;
              }
              details.push({
                label: task.label,
                latency: detailLatency,
                success: false,
                statusText: text,
                answers: [],
              });
              continue;
            }
            const data = await response.json();
            if (data.Status === 0) {
              const answers: NetworkAnswer[] = Array.isArray(data.Answer)
                  ? data.Answer.map((item: { data: string; TTL: number; type: number }) => ({
                    data: item.data,
                    ttl: item.TTL ?? 0,
                    type: item.type,
                    typeName: getRecordTypeName(item.type),
                  }))
                  : [];
              const hasAnswer = answers.length > 0;
              const statusText = hasAnswer
                  ? `${task.typeName} 解析成功 (${answers.length} 条记录)`
                  : `${task.typeName} 无可用记录 (NOERROR)`;
              if (!hasAnswer && data.Comment && !fallbackMessage) {
                fallbackMessage = `${task.typeName} 查询提示：${data.Comment}`;
              } else if (!hasAnswer && !fallbackMessage) {
                fallbackMessage = statusText;
              }
              details.push({
                label: task.label,
                latency: detailLatency,
                success: hasAnswer,
                statusText,
                answers,
              });
            } else {
              const text = `${task.typeName} 查询失败：${dnsStatusText(data.Status)}`;
              if (data.Comment && !fallbackMessage) {
                fallbackMessage = `${text} · ${data.Comment}`;
              } else if (!fallbackMessage) {
                fallbackMessage = text;
              }
              details.push({
                label: task.label,
                latency: detailLatency,
                success: false,
                statusText: text,
                answers: [],
              });
            }
          } catch (error) {
            const detailLatency = Math.round(performance.now() - detailStart);
            const text = `${task.typeName} 查询异常：${error instanceof Error ? error.message : String(error)}`;
            if (!fallbackMessage) {
              fallbackMessage = text;
            }
            details.push({
              label: task.label,
              latency: detailLatency,
              success: false,
              statusText: text,
              answers: [],
            });
          }
        }
      } finally {
        const latency = Math.round(performance.now() - overallStart);
        const totalRecords = details.reduce((acc, item) => acc + item.answers.length, 0);
        const success = details.some((detail) => detail.success);
        let statusText = success
            ? `已获取 ${totalRecords} 条解析记录`
            : fallbackMessage || '未能获取有效解析记录';
        if (success && totalRecords === 0) {
          statusText = fallbackMessage || '解析完成，但未返回记录';
        }
        networkTestState.result = {
          target: normalizedTarget,
          latency,
          timestamp: new Date().toLocaleString(),
          success,
          statusText,
          queryNote: note,
          details,
        };
        networkTestState.loading = false;
      }
    };

    const resetNetworkTest = () => {
      if (networkTestState.loading) {
        return;
      }
      networkTestState.target = '';
      networkTestState.error = '';
      networkTestState.result = null;
    };

    const handleMenuClick = ({ key }: { key: string }) => {
      if (key === 'ipv4' || key === 'ipv6' || key === 'network' || key === 'dhcp') {
        activeKey.value = key;
      }
    };

    const onIpv4AddressPrefixChange = (value: number | null) => {
      ipv4AddressState.prefixLength = clamp(value, 0, 32);
    };

    return {
      activeKey,
      ipv4State,
      ipv4AddressState,
      ipv6State,
      networkTestState,
      dhcpDetectionState,
      ipv4Summary,
      ipv6Summary,
      dhcpStatusText,
      dhcpDetectionSummary,
      dhcpPositiveCount,
      ipv4Analysis,
      handleMenuClick,
      onIpv4MaskBlur,
      onIpv4PrefixChange,
      onIpv4SplitChange,
      onIpv4AddressPrefixChange,
      onIpv6PrefixChange,
      onIpv6SplitChange,
      runNetworkTest,
      resetNetworkTest,
      runDhcpDetection,
      resetDhcpDetection,
      Empty,
    };
  },
});
</script>

<style scoped>
.ip-planner {
  padding: 24px;
}

.planner-header {
  background: #fff;
  margin-bottom: 16px;
  border-radius: 8px;
}

.tool-layout {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  min-height: 520px;
}

.tool-sider {
  background: #f5f5f5;
  padding: 16px 0;
}

.tool-sider__title {
  padding: 0 24px 12px;
  font-weight: 600;
  color: #1f1f1f;
}

.tool-content {
  padding: 24px;
}

.panel-description {
  color: #595959;
  margin-bottom: 16px;
}

.analysis-form {
  margin-bottom: 16px;
}

.analysis-alert {
  margin-bottom: 16px;
}

.dhcp-actions {
  margin-bottom: 16px;
}

.dhcp-empty {
  margin-top: 16px;
}

.dhcp-result-list {
  margin-top: 16px;
  display: grid;
  gap: 16px;
}

.dhcp-result {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.dhcp-result__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.dhcp-result__title h4 {
  margin: 0;
  font-size: 14px;
}

.dhcp-result__target {
  display: block;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  margin-top: 4px;
}

.dhcp-result__status {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  white-space: nowrap;
}

.dhcp-result__status.is-reachable {
  background: #fff1f0;
  color: #cf1322;
}

.dhcp-result__status.is-timeout {
  background: #f0f5ff;
  color: #1d39c4;
}

.dhcp-result__status.is-blocked {
  background: #fffbe6;
  color: #d48806;
}

.dhcp-result__status.is-error {
  background: #fff1f0;
  color: #a8071a;
}

.dhcp-result__note {
  margin: 12px 0 0;
  color: rgba(0, 0, 0, 0.65);
  line-height: 1.6;
}

.analysis-table {
  margin-top: 0;
}

.network-form__actions {
  display: flex;
  align-items: flex-end;
}

.network-detail {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  margin-bottom: 16px;
  background: #fafafa;
}

.network-detail__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.network-detail__header h4 {
  margin: 0;
  font-size: 16px;
  color: #1f1f1f;
}

.network-detail__status {
  font-size: 13px;
  color: #8c8c8c;
}

.network-detail__status.is-success {
  color: #389e0d;
}

.network-detail__status.is-warning {
  color: #d48806;
}

.network-detail__body ul {
  margin: 0;
  padding-left: 18px;
  color: #434343;
}

.network-detail__body li {
  margin-bottom: 6px;
}

.network-detail__body li:last-child {
  margin-bottom: 0;
}

.network-answer__record {
  font-weight: 500;
  margin-right: 12px;
}

.network-answer__meta {
  color: #8c8c8c;
  font-size: 12px;
}

.result-grid {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.result-item {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  box-shadow: inset 0 0 0 1px #f0f0f0;
}

.result-item h4 {
  margin-bottom: 8px;
  color: #262626;
}

.result-item p {
  font-size: 18px;
  margin: 0;
  color: #141414;
}

.result-item small {
  display: block;
  margin-top: 4px;
  color: #8c8c8c;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

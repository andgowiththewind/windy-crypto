<template>
  <div>
    <!--table-->
    <el-table
        size="mini"
        :fit="true"
        :stripe="true"
        :border="true"
        :row-style="rowStyleFnVal"
        :cell-style="cellStyleFnVal"
        :header-cell-style="headerCellStyleFnVal"
        :data="tableData"
        style="width: 100%;min-height:500px;">
      <el-table-column label="ID" width="170" :show-overflow-tooltip="true">
        <template v-slot="scope">
          <span
              :id="getIdCellId(scope.row)"
              :data-clipboard-text="scope.row.id"
              @click="idClickCopy(scope.row)">
            {{ scope.row.id }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('i18n_1826863045788438528')" width="150" prop="name" :show-overflow-tooltip="true">
        <template v-slot="scope">
          <span :id="getFilenameCellId(scope.row)" :data-clipboard-text="scope.row.name" @click="filenameClickCopy(scope.row)">{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('i18n_1827913996976656384')" width="100" prop="sizeLabel" :show-overflow-tooltip="true"></el-table-column>
      <!--操作-->
      <el-table-column :label="$t('i18n_1827946899051778052')" width="380" prop="_operation">
        <template v-slot="scope">
          <div v-if="scope.row.code==-1">
            <span>{{ $t('i18n_1827950850597851137') }}</span>
          </div>
          <div v-else-if="scope.row.code==0">
            <el-button-group v-if="scope.row.hadEncrypted">
              <el-button type="warning" plain size="mini" @click="decryptOne(scope.row,false)">{{ $t('i18n_1827961313217875969') }}</el-button>
              <el-button type="danger" plain size="mini" @click="decryptOne(scope.row,true)">{{ $t('i18n_1828982632629800960') }}&nbsp;{{ $t('i18n_1827961313217875969') }}</el-button>
            </el-button-group>
            <el-button-group v-else>
              <el-button type="success" plain size="mini" @click="encryptOne(scope.row,false)">{{ $t('i18n_1827961313217875968') }}</el-button>
              <el-button type="warning" plain size="mini" @click="encryptOne(scope.row,true)">{{ $t('i18n_1828969678349930496') }}</el-button>
            </el-button-group>
          </div>
          <div v-else-if="scope.row.code==10">
            <span>{{ $t('i18n_1827950850597851138') }}</span>
          </div>
          <div v-else-if="scope.row.code>=20 && scope.row.code<=40">
            <span>{{ $t('i18n_1827950850597851139') }}</span>
          </div>
          <div v-else>
            <span>{{ $t('i18n_1827950850597851136') }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('i18n_1827913996976656385')" prop="latestMsg" :show-overflow-tooltip="true"></el-table-column>
      <!--进度-->
      <el-table-column :label="$t('i18n_1827913996980850688')" prop="percentage">
        <template v-slot="scope">
          <div v-if="scope.row.code>=20 && scope.row.code<=40">
            <el-progress v-if="scope.row.percentage" :percentage="scope.row.percentage||0"></el-progress>
          </div>
          <span v-else style="opacity: 25%">/</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('i18n_1827913996980850689')" :show-overflow-tooltip="true">
        <template v-slot="scope">
          <span
              :id="getAbsPathCellId(scope.row)"
              :data-clipboard-text="scope.row.absPath"
              @click="absPathClickCopy(scope.row)">
            {{ scope.row.absPath }}
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import * as Methods from '@/config/Methods';
import {devConsoleLog} from "@/utils/commonUtils";
import {cryptoSubmitFn, rowStyleFn, cellStyleFn, headerCellStyleFn} from "@/api/insightTableApi";

export default {
  name: "InsightTable",
  components: {},
  data() {
    return {
      rowStyleFnVal: rowStyleFn,
      cellStyleFnVal: cellStyleFn,
      headerCellStyleFnVal: headerCellStyleFn,
      tableData: [],
      userPasswordCopy: '',
    }
  },// data
  methods: {
    getAbsPathCellId(row) {
      return 'absPathCellId_' + row.id;
    },
    absPathClickCopy(row) {
      let absPathCellClipboard = new this.clipboard("#" + this.getAbsPathCellId(row));
      absPathCellClipboard.on('success', () => {
        Message.success(this.$t('i18n_1827913996980850689') + " " + this.$t('i18n_1827946899051778048'));
        absPathCellClipboard.destroy();
      });
      absPathCellClipboard.on('error', () => {
        Message.error(this.$t('i18n_1827913996980850689') + " " + this.$t('i18n_1827946899051778049'));
        absPathCellClipboard.destroy();
      });
    },
    getIdCellId(row) {
      return 'idCellId_' + row.id;
    },
    getFilenameCellId(row) {
      return 'filenameCellId_' + row.id;
    },
    idClickCopy(row) {
      let idCellClipboard = new this.clipboard("#" + this.getIdCellId(row));
      idCellClipboard.on('success', () => {
        Message.success("ID " + this.$t('i18n_1827946899051778048'));
        idCellClipboard.destroy();
      });
      idCellClipboard.on('error', () => {
        Message.error("ID " + this.$t('i18n_1827946899051778049'));
        idCellClipboard.destroy();
      });
    },
    filenameClickCopy(row) {
      let filenameCellClipboard = new this.clipboard("#" + this.getFilenameCellId(row));
      filenameCellClipboard.on('success', () => {
        Message.success(this.$t('i18n_1826863045788438528') + " " + this.$t('i18n_1827946899051778048'));
        filenameCellClipboard.destroy();
      });
      filenameCellClipboard.on('error', () => {
        Message.error(this.$t('i18n_1826863045788438528') + " " + this.$t('i18n_1827946899051778049'));
        filenameCellClipboard.destroy();
      });
    },
    decryptOne(row, ignoreMissingHiddenFilename) {
      // 要求更新密码
      this.$bus.$emit(Methods.FN_CONTRACT_USER_PASSWORD);
      // 提交解密请求
      let cryptoSubmitPayload = {
        windyPathList: [row.absPath],
        dirPathList: [],
        askEncrypt: false,
        userPassword: this.userPasswordCopy,
        ignoreMissingHiddenFilename: ignoreMissingHiddenFilename,
      };
      cryptoSubmitFn(cryptoSubmitPayload).then(res => {
        Notification.success({title: this.$t('i18n_1827961313217875969'), message: res.msg, position: 'bottom-right'});
      });
    },
    encryptOne(row, isRequireCoverName) {
      // 要求更新密码
      this.$bus.$emit(Methods.FN_CONTRACT_USER_PASSWORD);
      // 提交加密请求
      let cryptoSubmitPayload = {
        windyPathList: [row.absPath],
        dirPathList: [],
        askEncrypt: true,
        userPassword: this.userPasswordCopy,
        isRequireCoverName: isRequireCoverName,
      };
      cryptoSubmitFn(cryptoSubmitPayload).then(res => {
        Notification.success({title: this.$t('i18n_1827961313217875968'), message: res.msg, position: 'bottom-right'});
      }).finally().catch();
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    // 收到tableData
    this.$bus.$on(Methods.FN_UPDATE_INSIGHT_TABLE_DATA, (data) => this.tableData = data);
    this.$bus.$on(Methods.FN_UPDATE_USER_PASSWORD, (data) => this.userPasswordCopy = data);
  },// mounted
}
</script>

<style scoped>
</style>
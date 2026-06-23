export interface IBackendRes<T> {
  error?: string | string[];
  message: string;
  statusCode: number | string;
  data?: T;
}

export interface IModelPaginate<T> {
  meta: {
    page: number;
    pageSize: number;
    pages: number;
    total: number;
  };
  result: T[];
}

export interface IAccount {
  access_token: string;
  user: {
    id: string;
    email: string;
    name: string;
    role: {
      id: string;
      name: string;
      permissions: {
        id: string;
        name: string;
        apiPath: string;
        method: string;
        module: string;
      }[];
    };
  };
}

export interface IGetAccount extends Omit<IAccount, "access_token"> {}

export interface ICompany {
  id?: string;
  name?: string;
  address?: string;
  logo: string;
  description?: string;
  createdBy?: string;
  isDeleted?: boolean;
  deletedAt?: boolean | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ISkill {
  id?: string;
  name?: string;
  createdBy?: string;
  isDeleted?: boolean;
  deletedAt?: boolean | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface IUser {
  id?: string;
  name: string;
  email: string;
  password?: string;
  age: number;
  gender: string;
  address: string;
  role?: {
    id: string;
    name: string;
  };

  company?: {
    id: string;
    name: string;
  };
  createdBy?: string;
  isDeleted?: boolean;
  deletedAt?: boolean | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface IJob {
  id?: string;
  name: string;
  skills: ISkill[];
  company?: {
    _id?: string | number; // Hoặc id tùy database của bạn
    id: string;
    name: string;
    logo?: string;
    address?: string;
  };
  location: string;
  salary: number;
  quantity: number;
  level: string;
  description: string;
  startDate: Date;
  endDate: Date;
  active: boolean;

  createdBy?: string;
  isDeleted?: boolean;
  deletedAt?: boolean | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface IResume {
  id?: string;
  email: string;
  userId: string;
  url: string;
  status: string;
  companyId:
    | string
    | {
        id: string;
        name: string;
        logo: string;
      };
  jobId:
    | string
    | {
        id: string;
        name: string;
      };
  history?: {
    status: string;
    updatedAt: Date;
    updatedBy: { id: string; email: string };
  }[];
  createdBy?: string;
  isDeleted?: boolean;
  deletedAt?: boolean | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface IPermission {
  id?: string;
  name?: string;
  apiPath?: string;
  method?: string;
  module?: string;

  createdBy?: string;
  isDeleted?: boolean;
  deletedAt?: boolean | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface IRole {
  id?: string;
  name: string;
  description: string;
  active: boolean;
  permissions: IPermission[] | string[];

  createdBy?: string;
  isDeleted?: boolean;
  deletedAt?: boolean | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ISubscribers {
  id?: string;
  name?: string;
  email?: string;
  skills: string[];
  createdBy?: string;
  isDeleted?: boolean;
  deletedAt?: boolean | null;
  createdAt?: string;
  updatedAt?: string;
}

// ===============================
// AI FEATURES — CV Doctor
// ===============================
export interface ICvAnalysisSuggestion {
  category: string;   // FORMAT, CONTENT, KEYWORD, IMPACT
  priority: string;   // HIGH, MEDIUM, LOW
  issue: string;
  suggestion: string;
}

export interface ICvAnalysis {
  id: number;
  fileName: string;
  overallScore: number;
  formatScore: number;
  contentScore: number;
  keywordScore: number;
  impactScore: number;
  summary: string;
  strengths: string[];
  suggestions: ICvAnalysisSuggestion[];
  createdAt: string;
}

export interface ICvHistory {
  id: number;
  fileName: string;
  overallScore: number;
  createdAt: string;
}

export interface ICvMatch {
  resumeId: number;
  jobId: number;
  jobName: string;
  matchScore: number;
  summary: string;
  matchedSkills: string[];
  missingSkills: string[];
  recommendations: string[];
  cached?: boolean;
}

// ===============================
// AI FEATURES — Interview Coach
// ===============================
export interface IInterviewQuestion {
  sessionId: number;
  questionNumber: number;
  totalQuestions: number;
  question: string;
  category: string;    // TECHNICAL, BEHAVIORAL, SYSTEM_DESIGN
  difficulty: string;  // EASY, MEDIUM, HARD
}

export interface IAnswerFeedback {
  sessionId: number;
  questionNumber: number;
  score: number;
  feedback: string;
  betterAnswer: string;
  lastQuestion: boolean;
  nextQuestion: IInterviewQuestion | null;
}

export interface IInterviewSummary {
  sessionId: number;
  jobPosition: string;
  level: string;
  overallScore: number;
  finalSummary: string;
  questions: {
    questionNumber: number;
    question: string;
    answer: string;
    score: number;
    feedback: string;
  }[];
  createdAt: string;
}

export interface IInterviewHistory {
  sessionId: number;
  jobPosition: string;
  level: string;
  overallScore: number;
  status: string;
  createdAt: string;
}
